package com.integration_service.service;

import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.dto.EventRequest;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.handler.IntegrationTypeResolver;
import com.integration_service.service.integrationService.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualExecutionService {

    private final List<IntegrationHandler> handlers;
    private final IntegrationConfigService configService;
    private final ExecutionLogService logService;

    public Object execute(IntegrationType service, Map<String, Object> data) {
        IntegrationHandler handler = handlers.stream()
                .filter(h -> h.getService().equals(service))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No IntegrationHandler registered for manual execution type: {}", service);
                    return new RuntimeException("Handler not found");
                });

        IntegrationType configKey = IntegrationTypeResolver.configKey(service);

        TenantIntegration config = configService.getByService(configKey)
                .orElseThrow(() -> new RuntimeException("Integration not configured"));

        if (!config.isEnabled()) {
            throw new RuntimeException("Integration disabled");
        }

        if (!"MANUAL".equalsIgnoreCase(config.getMode())
                && !"HYBRID".equalsIgnoreCase(config.getMode())) {
            throw new RuntimeException("Manual execution not allowed");
        }

        log.info("Manual execution via handler {} (config key: {})",
                handler.getClass().getSimpleName(), configKey);

        EventRequest event = new EventRequest();
        event.setEventType("MANUAL_TRIGGER");
        event.setTenantId(com.integration_service.common.config.TenantContext.getTenant());
        event.setPayload(data);

        try {
            Object response = handler.execute(event, config);
            logService.logSuccess(service, "MANUAL_TRIGGER", data, response);
            return response;
        } catch (Exception ex) {
            logService.logFailure(service, "MANUAL_TRIGGER", data, ex);
            throw ex;
        }
    }
}
