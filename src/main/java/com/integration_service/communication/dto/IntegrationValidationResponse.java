package com.integration_service.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
    public class IntegrationValidationResponse {

        private boolean success;
        private String message;
    private Map<String, Boolean> checks;

    private Map<String, Object> metadata;
    }