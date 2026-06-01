package com.integration_service.dto.integrationDto;

import com.integration_service.communication.entity.IntegrationHealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationHealthDetails {

    private IntegrationHealthStatus healthStatus;
    private LocalDateTime lastValidatedAt;
    private LocalDateTime lastHealthCheckAt;
    private String lastError;
    private Boolean reauthRequired;
    private Integer consecutiveFailures;
}
