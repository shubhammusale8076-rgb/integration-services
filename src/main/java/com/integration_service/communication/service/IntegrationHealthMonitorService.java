package com.integration_service.communication.service;

import com.integration_service.communication.dto.IntegrationHealthResult;
import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.handler.IntegrationHandlerHealthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationHealthMonitorService {

    private static final Set<com.integration_service.communication.entity.IntegrationType> MONITORED_TYPES = Set.of(
            com.integration_service.communication.entity.IntegrationType.WHATSAPP,
            com.integration_service.communication.entity.IntegrationType.RAZORPAY,
            com.integration_service.communication.entity.IntegrationType.GOOGLE
    );

    private final TenantIntegrationRepository repository;
    private final List<IntegrationHandler> handlers;

    @Value("${integration.health.degraded-threshold:3}")
    private int degradedThreshold;

    @Scheduled(cron = "0 */30 * * * *")
    public void runScheduledHealthChecks() {
        log.info("Integration health check started");
        List<TenantIntegration> integrations = repository.findByStatusAndEnabledTrue(IntegrationStatus.CONNECTED);

        log.info("Found {} active connected integration(s) for health monitoring", integrations.size());

        for (TenantIntegration integration : integrations) {
            if (!MONITORED_TYPES.contains(integration.getIntegrationType())) {
                log.debug("Skipping health check for unsupported type: {}", integration.getIntegrationType());
                continue;
            }
            try {
                checkIntegrationHealth(integration.getId());
            } catch (Exception ex) {
                log.error("Unexpected error during health check for integration {}: {}",
                        integration.getId(), ex.getMessage(), ex);
            }
        }

        log.info("Integration health check completed");
    }

    @Transactional
    public IntegrationHealthResult checkIntegrationHealth(java.util.UUID integrationId) {
        TenantIntegration integration = repository.findById(integrationId)
                .orElseThrow(() -> new IllegalArgumentException("Integration not found: " + integrationId));

        return checkIntegrationHealth(integration);
    }

    @Transactional
    public IntegrationHealthResult checkIntegrationHealth(TenantIntegration integration) {
        IntegrationHandlerHealthContext.clear();

        log.info("Health check started: tenant={}, type={}, integrationId={}",
                integration.getTenantId(), integration.getIntegrationType(), integration.getId());

        IntegrationHandler handler = resolveHandler(integration.getIntegrationType());
        if (handler == null) {
            log.error("No IntegrationHandler registered for health check type: {}", integration.getIntegrationType());
            IntegrationHealthResult result = IntegrationHealthResult.builder()
                    .healthy(false)
                    .status(com.integration_service.communication.entity.IntegrationHealthStatus.FAILED)
                    .reauthRequired(false)
                    .error("No handler registered for " + integration.getIntegrationType())
                    .checkedAt(java.time.LocalDateTime.now())
                    .build();
            applyHealthResult(integration, result);
            return result;
        }

        if (!handler.supportsHealthMonitoring()) {
            log.debug("Handler {} does not support health monitoring", handler.getClass().getSimpleName());
            return IntegrationHealthResult.builder()
                    .healthy(true)
                    .status(com.integration_service.communication.entity.IntegrationHealthStatus.CONNECTED)
                    .reauthRequired(false)
                    .checkedAt(java.time.LocalDateTime.now())
                    .build();
        }

        log.info("Provider resolved for health check: {} -> {}",
                integration.getIntegrationType(), handler.getClass().getSimpleName());

        IntegrationHealthResult result = handler.validateHealth(integration);
        applyHealthResult(integration, result);

        if (result.isHealthy()) {
            log.info("Token validation success: tenant={}, type={}",
                    integration.getTenantId(), integration.getIntegrationType());
        } else if (result.getStatus() == com.integration_service.communication.entity.IntegrationHealthStatus.TOKEN_EXPIRED) {
            log.warn("Token expired: tenant={}, type={}, error={}",
                    integration.getTenantId(), integration.getIntegrationType(), result.getError());
        } else if (result.isReauthRequired()) {
            log.warn("Re-authentication required: tenant={}, type={}, error={}",
                    integration.getTenantId(), integration.getIntegrationType(), result.getError());
        } else {
            log.warn("Health check failed: tenant={}, type={}, status={}, error={}",
                    integration.getTenantId(), integration.getIntegrationType(),
                    result.getStatus(), result.getError());
        }

        return result;
    }

    private void applyHealthResult(TenantIntegration integration, IntegrationHealthResult result) {
        if (result.isHealthy()) {
            integration.applySuccessfulHealthCheck(result.getCheckedAt());
            log.debug("Consecutive failures reset for integration {}", integration.getId());
        } else {
            int previousFailures = integration.getConsecutiveFailures() != null ? integration.getConsecutiveFailures() : 0;
            integration.applyFailedHealthCheck(result, degradedThreshold);
            log.info("Consecutive failure incremented: integrationId={}, failures={}->{}",
                    integration.getId(), previousFailures, integration.getConsecutiveFailures());
            if (integration.getHealthStatus() == com.integration_service.communication.entity.IntegrationHealthStatus.DEGRADED) {
                log.warn("Integration marked DEGRADED: integrationId={}, tenant={}, type={}",
                        integration.getId(), integration.getTenantId(), integration.getIntegrationType());
            }
        }
        repository.save(integration);
    }

    private IntegrationHandler resolveHandler(com.integration_service.communication.entity.IntegrationType type) {
        return handlers.stream()
                .filter(h -> h.getService() == type)
                .findFirst()
                .orElse(null);
    }
}
