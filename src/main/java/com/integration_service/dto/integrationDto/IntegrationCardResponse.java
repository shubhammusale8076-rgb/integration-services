package com.integration_service.dto.integrationDto;

import com.integration_service.communication.entity.IntegrationHealthStatus;
import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.enums.IntegrationAuthType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class IntegrationCardResponse {

    private UUID id;
    private IntegrationType service;
    private String displayName;
    private String description;
    private String icon;
    private String iconColor;
    private String iconBg;
    private IntegrationAuthType authType;
    private boolean connected;
    private boolean enabled;
    private IntegrationStatus status;
    private IntegrationHealthStatus healthStatus;
    private LocalDateTime lastValidatedAt;
    private LocalDateTime lastHealthCheckAt;
    private String lastError;
    private Boolean reauthRequired;
    private Integer consecutiveFailures;
}
