package com.integration_service.dto.integrationDto;

import com.integration_service.communication.entity.IntegrationHealthStatus;
import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.enums.IntegrationAuthType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class IntegrationCatalogResponse {

    private UUID id;
    private IntegrationType service;
    private String displayName;
    private String description;
    private String icon;
    private String iconColor;
    private String iconBg;
    private IntegrationAuthType authType;
    private boolean active;
    private String configSchema;
    private boolean connected;
    private boolean enabled;
    private String mode;
    private IntegrationStatus status;
    private String email;
    private LocalDateTime connectedAt;
    private LocalDateTime updatedAt;
    private boolean supportsWebhooks;
    private String webhookUrl;
    private boolean supportsEvents;
    private boolean supportsOAuthReconnect;
    private List<String> supportedEvents;
    private Map<String, Object> metadata;
    private IntegrationHealthStatus healthStatus;
    private LocalDateTime lastValidatedAt;
    private LocalDateTime lastHealthCheckAt;
    private String lastError;
    private Boolean reauthRequired;
    private Integer consecutiveFailures;
}
