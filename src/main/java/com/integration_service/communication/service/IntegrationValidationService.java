package com.integration_service.communication.service;

import com.integration_service.communication.dto.IntegrationValidationRequest;
import com.integration_service.communication.dto.IntegrationValidationResponse;
import com.integration_service.handler.IntegrationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationValidationService {

    private final List<IntegrationHandler> handlers;

    public IntegrationValidationResponse validate(IntegrationValidationRequest request) {
        IntegrationHandler handler = handlers.stream()
                .filter(h -> h.getService() == request.getIntegrationType())
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No IntegrationHandler registered for validation type: {}", request.getIntegrationType());
                    return new RuntimeException("Unsupported integration type: " + request.getIntegrationType());
                });

        log.info("Resolved validation handler: {} for type: {}",
                handler.getClass().getSimpleName(), request.getIntegrationType());

        IntegrationValidationResponse response = handler.validateIntegration(request);

        if (response.isSuccess()) {
            log.info("Integration validation successful for type: {}", request.getIntegrationType());
        } else {
            log.warn("Integration validation failed for type {}: {}",
                    request.getIntegrationType(), response.getMessage());

        }

        return response;
    }
}
