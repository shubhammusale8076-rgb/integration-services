package com.integration_service.dto;


import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class IntegrationRequest {
    private UUID tenantId;
    private String provider;
    private Map<String, Object> config;
}
