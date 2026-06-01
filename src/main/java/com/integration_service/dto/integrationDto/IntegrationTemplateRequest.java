package com.integration_service.dto.integrationDto;

import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.enums.IntegrationAuthType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationTemplateRequest {

    private IntegrationType service;
    private String displayName;
    private String description;
    private String icon;
    private String iconColor;
    private String iconBg;
    private IntegrationAuthType authType;
    private String configSchema;
    private Boolean active;
}
