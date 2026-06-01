package com.integration_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.dto.integrationDto.IntegrationCardResponse;
import com.integration_service.dto.integrationDto.IntegrationCatalogResponse;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.repository.IntegrationTemplateRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationCatalogService {

    private final IntegrationTemplateRepo integrationTemplateRepo;
    private final TenantIntegrationRepository tenantIntegrationRepository;
    private final ObjectMapper objectMapper;
    private final com.integration_service.mapper.IntegrationHealthMapper healthMapper;

    public List<IntegrationCardResponse> getCatalog(UUID tenantId) {

        log.info("Fetching integration catalog for tenant: {}", tenantId);

        List<IntegrationTemplate> activeTemplates = integrationTemplateRepo.findByActiveTrue();
        
        List<TenantIntegration> tenantIntegrations = tenantIntegrationRepository.findByTenantId(tenantId);
        
        Map<com.integration_service.communication.entity.IntegrationType, TenantIntegration> tenantIntegrationMap = tenantIntegrations.stream()
                .collect(Collectors.toMap(TenantIntegration::getIntegrationType, ti -> ti));

        return activeTemplates.stream().map(template -> {
            TenantIntegration ti = tenantIntegrationMap.get(template.getService());

            // =========================================
            boolean connected = false;
            boolean enabled = false;
            IntegrationStatus status = null;


            if (ti != null) {

                status = ti.getStatus();

                connected =
                        status == IntegrationStatus.CONNECTED;

                enabled = ti.isEnabled();



            }

            var health = healthMapper.toDetails(ti);

            return IntegrationCardResponse.builder()
                    .id(template.getId())
                    .service(template.getService())
                    .displayName(template.getDisplayName())
                    .description(template.getDescription())
                    .icon(template.getIcon())
                    .iconColor(template.getIconColor())
                    .iconBg(template.getIconBg())
                    .authType(template.getAuthType())
                    .connected(connected)
                    .enabled(enabled)
                    .status(status)
                    .healthStatus(health != null ? health.getHealthStatus() : null)
                    .lastValidatedAt(health != null ? health.getLastValidatedAt() : null)
                    .lastHealthCheckAt(health != null ? health.getLastHealthCheckAt() : null)
                    .lastError(health != null ? health.getLastError() : null)
                    .reauthRequired(health != null ? health.getReauthRequired() : null)
                    .consecutiveFailures(health != null ? health.getConsecutiveFailures() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    public IntegrationCatalogResponse getDetails(UUID tenantId, IntegrationType service) {

        log.info("Fetching integration details. tenant={}, service={}", tenantId, service);

        IntegrationTemplate template = integrationTemplateRepo.findByService(service)
                        .orElseThrow(() -> new RuntimeException("Integration template not found"));

        TenantIntegration ti = tenantIntegrationRepository.findByTenantIdAndIntegrationType(tenantId, service)
                        .orElse(null);

        boolean connected = false;
        boolean enabled = false;
        String mode = null;
        IntegrationStatus status = null;
        String email = null;
        Map<String, Object> metadata = null;

        if (ti != null) {
            status = ti.getStatus();
            connected = status == IntegrationStatus.CONNECTED;
            enabled = ti.isEnabled();
            mode = ti.getMode();

            // =====================================
            // METADATA
            // =====================================
            try {

                if (ti.getMetadata() != null) {
                    metadata = objectMapper.readValue(ti.getMetadata(), new TypeReference<>() {});

                    if (metadata.containsKey("email")) {
                        email = String.valueOf(metadata.get("email"));
                    }
                }

            } catch (Exception ex) {
                log.error("Failed to parse metadata", ex);
            }
        }

        // =====================================
        // FEATURES
        // =====================================
        boolean supportsWebhooks = service == IntegrationType.RAZORPAY || service == IntegrationType.WHATSAPP;
        boolean supportsEvents = true;
        boolean supportsOAuthReconnect = template.getAuthType().name().equals("OAUTH");

        List<String> supportedEvents =
                switch (service) {

                    case GOOGLE -> List.of(
                            "Calendar Sync",
                            "Gmail Automation",
                            "Sheets Reporting"
                    );

                    case RAZORPAY -> List.of(
                            "Payment Success",
                            "Payment Failed",
                            "Refund Created"
                    );

                    case WHATSAPP -> List.of(
                            "Lead Reminder",
                            "Membership Reminder",
                            "Promotional Broadcast"
                    );

                    default -> List.of();
                };

        var health = healthMapper.toDetails(ti);

        return IntegrationCatalogResponse.builder()
                .id(template.getId())
                .service(template.getService())
                .displayName(template.getDisplayName())
                .description(template.getDescription())
                .icon(template.getIcon())
                .iconColor(template.getIconColor())
                .iconBg(template.getIconBg())
                .authType(template.getAuthType())
                .active(template.isActive())
                .configSchema(template.getConfigSchema())
                .connected(connected)
                .enabled(enabled)
                .mode(mode)
                .status(status)
                .email(email)
                .metadata(metadata)
                .connectedAt(ti != null ? ti.getCreatedAt() : null)
                .updatedAt(ti != null ? ti.getUpdatedAt() : null)
                .supportsWebhooks(supportsWebhooks)
                .supportsEvents(supportsEvents)
                .supportsOAuthReconnect(supportsOAuthReconnect)
                .supportedEvents(supportedEvents)
                .healthStatus(health != null ? health.getHealthStatus() : null)
                .lastValidatedAt(health != null ? health.getLastValidatedAt() : null)
                .lastHealthCheckAt(health != null ? health.getLastHealthCheckAt() : null)
                .lastError(health != null ? health.getLastError() : null)
                .reauthRequired(health != null ? health.getReauthRequired() : null)
                .consecutiveFailures(health != null ? health.getConsecutiveFailures() : null)
                .build();
    }
}
