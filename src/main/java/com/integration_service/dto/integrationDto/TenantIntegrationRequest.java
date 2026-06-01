package com.integration_service.dto.integrationDto;

import com.integration_service.communication.entity.IntegrationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantIntegrationRequest {

    private IntegrationType service;
    private boolean enabled;
    private String mode;
    private String configJson;
}
