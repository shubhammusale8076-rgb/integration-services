package com.integration_service.communication.dto;

import com.integration_service.communication.entity.IntegrationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationValidationRequest {

    private UUID tenantId;
    private IntegrationType integrationType;
    private Map<String, Object> config;
    private Map<String, Object> testData;
}