package com.integration_service.mapper;

import com.integration_service.dto.integrationDto.IntegrationConfigResponse;
import com.integration_service.dto.integrationDto.TenantIntegrationRequest;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.entity.IntegrationStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IntegrationConfigMapper {

    private final IntegrationHealthMapper healthMapper;

    public IntegrationConfigMapper(IntegrationHealthMapper healthMapper) {
        this.healthMapper = healthMapper;
    }

    public TenantIntegration toEntity(TenantIntegrationRequest dto, String tenantId) {
        TenantIntegration entity = new TenantIntegration();

        entity.setTenantId(UUID.fromString(tenantId));
        entity.setIntegrationType(dto.getService());
        entity.setEnabled(dto.isEnabled());
        entity.setMode(dto.getMode());
        entity.setMetadata(dto.getConfigJson());
        entity.setStatus(IntegrationStatus.CONNECTED);
        entity.markConnectedHealth();

        return entity;
    }

    public IntegrationConfigResponse toResponse(TenantIntegration entity) {
        var health = healthMapper.toDetails(entity);
        return IntegrationConfigResponse.builder()
                .id(entity.getId())
                .service(entity.getIntegrationType())
                .enabled(entity.isEnabled())
                .mode(entity.getMode())
                .configJson(entity.getMetadata())
                .healthStatus(health != null ? health.getHealthStatus() : null)
                .lastValidatedAt(health != null ? health.getLastValidatedAt() : null)
                .lastHealthCheckAt(health != null ? health.getLastHealthCheckAt() : null)
                .lastError(health != null ? health.getLastError() : null)
                .reauthRequired(health != null ? health.getReauthRequired() : null)
                .consecutiveFailures(health != null ? health.getConsecutiveFailures() : null)
                .build();
    }
}
