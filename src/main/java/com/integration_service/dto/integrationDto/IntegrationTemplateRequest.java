package com.integration_service.dto.integrationDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationTemplateRequest {

    private String service;
    private boolean enabled;
    private String mode;
    private String configJson;
}
