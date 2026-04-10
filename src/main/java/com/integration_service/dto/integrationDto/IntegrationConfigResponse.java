package com.integration_service.dto.integrationDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IntegrationConfigResponse {
    private UUID id;
    private String service;
    private boolean enabled;
    private String mode;
    private String configJson;
}
