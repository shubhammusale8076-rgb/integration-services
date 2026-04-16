package com.integration_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class EventRequest {

    @NotBlank(message = "eventType is required")
    private String eventType;

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotNull(message = "payload is required")
    private Map<String, Object> payload;

    // Helper to return payload as 'data' for existing IntegrationHandlers
    public Map<String, Object> getData() {
        return payload;
    }
}
