package com.integration_service.communication.dto;

import com.integration_service.communication.entity.IntegrationHealthStatus;
import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.IntegrationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationResponse {
    private IntegrationType integrationType;
    private IntegrationStatus status;
    private String message;
    private IntegrationHealthStatus healthStatus;
    private LocalDateTime lastValidatedAt;
    private LocalDateTime lastHealthCheckAt;
    private String lastError;
    private Boolean reauthRequired;
    private Integer consecutiveFailures;
}
