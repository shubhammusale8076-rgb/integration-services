package com.integration_service.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.communication.dto.IntegrationRequest;
import com.integration_service.communication.dto.IntegrationResponse;
import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.repository.IntegrationTemplateRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final TenantIntegrationRepository repository;
    private final IntegrationTemplateRepo templateRepo;
    private final List<IntegrationHandler> handlers;
    private final ObjectMapper objectMapper;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    @Transactional
    public IntegrationResponse connectIntegration(IntegrationRequest request) {
        log.info("Connecting integration: {} for tenant: {}", request.getIntegrationType(), request.getTenantId());

        IntegrationType type = request.getIntegrationType();
        IntegrationHandler handler = getHandler(type);

        if (handler == null) {
            log.error("No IntegrationHandler registered for type: {}", type);
            return IntegrationResponse.builder()
                    .integrationType(type)
                    .status(IntegrationStatus.FAILED)
                    .message("Unsupported integration type: " + type)
                    .build();
        }

        log.info("Resolved provider handler: {} for type: {}", handler.getClass().getSimpleName(), type);

        try {
            log.info("Validating credentials for integration type: {}", type);
            handler.validate(request.getConfig());
            log.info("Validation successful for integration type: {}", type);

            handler.connect(request.getConfig());
            log.info("Provider connect completed for integration type: {}", type);

            Optional<TenantIntegration> existing = repository.findByTenantIdAndIntegrationType(request.getTenantId(), type);

            TenantIntegration integration;
            if (existing.isPresent()) {
                integration = existing.get();
                log.info("Updating existing integration for tenant: {}", request.getTenantId());
            } else {
                integration = new TenantIntegration();
                integration.setTenantId(request.getTenantId());
                integration.setIntegrationType(type);
                integration.setEnabled(true);
                integration.setMode("AUTOMATED");

                templateRepo.findByService(type).ifPresent(integration::setTemplate);

                log.info("Creating new integration for tenant: {}", request.getTenantId());
            }

            Map<String, Object> metadata = new HashMap<>(request.getConfig());

            if (type == IntegrationType.RAZORPAY) {

                String webhookUrl = publicBaseUrl + "/api/webhooks/razorpay";

                metadata.put("webhookUrl", webhookUrl);
            }
                integration.setMetadata(objectMapper.writeValueAsString(metadata)
            );
            integration.setStatus(IntegrationStatus.CONNECTED);
            integration.markConnectedHealth();
            integration.setUpdatedAt(LocalDateTime.now());

            repository.save(integration);

            log.info("Integration activated successfully for tenant: {}, type: {}", request.getTenantId(), type);
            return IntegrationResponse.builder()
                    .integrationType(type)
                    .status(IntegrationStatus.CONNECTED)
                    .message("Integration connected successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to connect integration type {}: {}", type, e.getMessage());
            return IntegrationResponse.builder()
                    .integrationType(type)
                    .status(IntegrationStatus.FAILED)
                    .message(e.getMessage())
                    .build();
        }
    }

    @Transactional
    public IntegrationResponse disconnectIntegration(IntegrationRequest request) {
        log.info("Disconnecting integration: {} for tenant: {}", request.getIntegrationType(), request.getTenantId());

        IntegrationType type = request.getIntegrationType();
        Optional<TenantIntegration> existing = repository.findByTenantIdAndIntegrationType(request.getTenantId(), type);

        if (existing.isPresent()) {
            TenantIntegration integration = existing.get();
            integration.setStatus(IntegrationStatus.DISCONNECTED);
            integration.markDisconnectedHealth();
            integration.setUpdatedAt(LocalDateTime.now());
            repository.save(integration);

            log.info("Integration disconnected for tenant: {}", request.getTenantId());
            return IntegrationResponse.builder()
                    .integrationType(type)
                    .status(IntegrationStatus.DISCONNECTED)
                    .message("Integration disconnected successfully")
                    .build();
        }

        return IntegrationResponse.builder()
                .integrationType(type)
                .status(IntegrationStatus.FAILED)
                .message("Integration not found")
                .build();
    }

    public List<IntegrationResponse> getTenantIntegrations(UUID tenantId) {
        log.info("Fetching integrations for tenant: {}", tenantId);
        return repository.findByTenantId(tenantId).stream()
                .map(this::toIntegrationResponse)
                .collect(Collectors.toList());
    }

    public Optional<TenantIntegration> getIntegration(UUID tenantId, IntegrationType type) {
        return repository.findByTenantIdAndIntegrationType(tenantId, type);
    }

    public Map<String, String> getConfigMap(TenantIntegration integration) {
        try {
            return objectMapper.readValue(integration.getMetadata(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Failed to parse integration config", e);
            return Map.of();
        }
    }

    private IntegrationHandler getHandler(IntegrationType type) {
        return handlers.stream()
                .filter(h -> h.getService() == type)
                .findFirst()
                .orElse(null);
    }

    private IntegrationResponse toIntegrationResponse(TenantIntegration integration) {
        return IntegrationResponse.builder()
                .integrationType(integration.getIntegrationType())
                .status(integration.getStatus())
                .message("Fetched successfully")
                .healthStatus(integration.getHealthStatus())
                .lastValidatedAt(integration.getLastValidatedAt())
                .lastHealthCheckAt(integration.getLastHealthCheckAt())
                .lastError(integration.getLastError())
                .reauthRequired(integration.getReauthRequired())
                .consecutiveFailures(integration.getConsecutiveFailures())
                .build();
    }
}
