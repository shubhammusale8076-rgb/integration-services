package com.integration_service.communication.controller;

import com.integration_service.communication.dto.IntegrationRequest;
import com.integration_service.communication.dto.IntegrationValidationRequest;
import com.integration_service.communication.dto.IntegrationValidationResponse;
import com.integration_service.communication.service.IntegrationService;
import com.integration_service.communication.service.IntegrationValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/integrations")
@RequiredArgsConstructor
public class InternalIntegrationController {

    private final IntegrationService integrationService;
    private final IntegrationValidationService validationService;

    @Value("${internal.api.secret}")
    private String internalSecret;

    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestHeader(value = "X-Internal-Secret", required = false) String secret,
                                    @RequestBody IntegrationRequest request) {
        if (!validateSecret(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid secret");
        }
        return ResponseEntity.ok(integrationService.connectIntegration(request));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect(@RequestHeader(value = "X-Internal-Secret", required = false) String secret,
                                       @RequestBody IntegrationRequest request) {
        if (!validateSecret(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid secret");
        }
        return ResponseEntity.ok(integrationService.disconnectIntegration(request));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<?> getIntegrations(@RequestHeader(value = "X-Internal-Secret", required = false) String secret,
                                            @PathVariable UUID tenantId) {
        if (!validateSecret(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid secret");
        }
        return ResponseEntity.ok(integrationService.getTenantIntegrations(tenantId));
    }

    @PostMapping("/validate")
    public ResponseEntity<IntegrationValidationResponse> validate(@RequestBody IntegrationValidationRequest request) {

        IntegrationValidationResponse response = validationService.validate(request);

        return ResponseEntity.ok(response);
    }

    private boolean validateSecret(String secret) {
        return internalSecret != null && internalSecret.equals(secret);
    }
}
