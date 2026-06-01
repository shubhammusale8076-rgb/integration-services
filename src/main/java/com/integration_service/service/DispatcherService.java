package com.integration_service.service;

import com.integration_service.common.config.TenantContext;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.dto.EventRequest;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.handler.IntegrationTypeResolver;
import com.integration_service.service.integrationService.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherService {

    private final List<IntegrationHandler> handlers;
    private final IntegrationConfigService configService;
    private final ExecutionLogService logService;

    @Async
    public void dispatch(EventRequest event, String tenantId) {
        TenantContext.setTenant(tenantId);
        try {
            List<TenantIntegration> configs = configService.getTenantConfig();

            if (configs.isEmpty()) {
                log.debug("No tenant integrations configured for tenant: {}", tenantId);
                return;
            }

            Map<IntegrationType, TenantIntegration> configMap = configs.stream()
                    .filter(TenantIntegration::isEnabled)
                    .collect(Collectors.toMap(
                            TenantIntegration::getIntegrationType,
                            config -> config,
                            (c1, c2) -> c1
                    ));

            for (IntegrationHandler handler : handlers) {
                IntegrationType configKey = IntegrationTypeResolver.configKey(handler.getService());
                TenantIntegration config = configMap.get(configKey);

                if (config == null) {
                    log.debug("Skipping handler {} — no config for key {}",
                            handler.getClass().getSimpleName(), configKey);
                    continue;
                }

                if (!"AUTOMATED".equalsIgnoreCase(config.getMode())
                        && !"HYBRID".equalsIgnoreCase(config.getMode())) {
                    log.debug("Skipping handler {} — mode {} does not allow automated dispatch",
                            handler.getClass().getSimpleName(), config.getMode());
                    continue;
                }

                if (!handler.supports(event.getEventType())) {
                    log.debug("Skipping handler {} — does not support event {}",
                            handler.getClass().getSimpleName(), event.getEventType());
                    continue;
                }

                log.info("Executing handler {} for event {} (config key: {})",
                        handler.getClass().getSimpleName(), event.getEventType(), configKey);

                try {
                    Object response = handler.execute(event, config);

                    logService.logSuccess(
                            handler.getService(),
                            event.getEventType(),
                            event.getData(),
                            response
                    );

                } catch (Exception ex) {
                    log.error("Handler {} failed for event {}: {}",
                            handler.getClass().getSimpleName(), event.getEventType(), ex.getMessage());

                    logService.logFailure(
                            handler.getService(),
                            event.getEventType(),
                            event.getData(),
                            ex
                    );
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            TenantContext.clear();
        }
    }
}
