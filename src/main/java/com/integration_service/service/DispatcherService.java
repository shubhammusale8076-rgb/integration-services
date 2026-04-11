package com.integration_service.service;

import com.integration_service.config.TenantContext;
import com.integration_service.dto.EventRequest;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.service.integrationService.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispatcherService {

    private final List<IntegrationHandler> handlers;
    private final IntegrationConfigService configService;
    private final ExecutionLogService logService;

    @Async
    public void dispatch(EventRequest event) {

        String tenantId = TenantContext.getTenant();

        List<IntegrationTemplate> configs = configService.getTenantConfig();

        if (configs.isEmpty()) {
            return;
        }

        // Convert configs → Map for faster lookup
        Map<String, IntegrationTemplate> configMap = configs.stream()
                .filter(config -> config.isEnabled())
                .collect(Collectors.toMap(
                        IntegrationTemplate::getService,
                        config -> config,
                        (c1, c2) -> c1 // handle duplicates
                ));

        for (IntegrationHandler handler : handlers) {

            IntegrationTemplate config = configMap.get(handler.getService());

            if (config == null) {
                continue;
            }

            if (!"AUTOMATED".equalsIgnoreCase(config.getMode())
                    && !"HYBRID".equalsIgnoreCase(config.getMode())) {
                continue;
            }

            if (!handler.supports(event.getEventType())) {
                continue;
            }

            try {
                Object response = handler.execute(event, config);

                logService.logSuccess(
                        handler.getService(),
                        event.getEventType(),
                        event.getData(),
                        response
                );

            } catch (Exception ex) {

                logService.logFailure(
                        handler.getService(),
                        event.getEventType(),
                        event.getData(),
                        ex
                );

                // Don't break loop — continue other integrations
            }
        }
    }
}
