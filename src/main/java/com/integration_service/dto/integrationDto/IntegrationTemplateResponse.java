package com.integration_service.dto.integrationDto;

import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.enums.IntegrationAuthType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationTemplateResponse {

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
