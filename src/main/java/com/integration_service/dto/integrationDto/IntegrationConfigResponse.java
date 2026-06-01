package com.integration_service.dto.integrationDto;

import com.integration_service.communication.entity.IntegrationHealthStatus;
import com.integration_service.communication.entity.IntegrationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IntegrationConfigResponse {
    private UUID id;
    private IntegrationType service;
    private boolean enabled;
    private String mode;
    private String configJson;
    private IntegrationHealthStatus healthStatus;
    private LocalDateTime lastValidatedAt;
    private LocalDateTime lastHealthCheckAt;
    private String lastError;
    private Boolean reauthRequired;
    private Integer consecutiveFailures;
}
