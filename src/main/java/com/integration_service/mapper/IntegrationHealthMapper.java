package com.integration_service.mapper;

import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.dto.integrationDto.IntegrationHealthDetails;
import org.springframework.stereotype.Component;

@Component
public class IntegrationHealthMapper {

    public IntegrationHealthDetails toDetails(TenantIntegration entity) {
        if (entity == null) {
            return null;
        }
        return IntegrationHealthDetails.builder()
                .healthStatus(entity.getHealthStatus())
                .lastValidatedAt(entity.getLastValidatedAt())
                .lastHealthCheckAt(entity.getLastHealthCheckAt())
                .lastError(entity.getLastError())
                .reauthRequired(entity.getReauthRequired())
                .consecutiveFailures(entity.getConsecutiveFailures())
                .build();
    }
}
