package com.integration_service.controller;

import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.dto.integrationDto.IntegrationTemplateRequest;
import com.integration_service.dto.integrationDto.IntegrationTemplateResponse;
import com.integration_service.service.IntegrationTemplateAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/integrations")
@RequiredArgsConstructor
public class AdminIntegrationController {

    private final IntegrationTemplateAdminService adminService;

    @PostMapping
    public ResponseEntity<IntegrationTemplateResponse> createIntegration(@RequestBody IntegrationTemplateRequest request) {
        IntegrationTemplateResponse response = adminService.createIntegration(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{service}")
    public ResponseEntity<IntegrationTemplateResponse> updateIntegration(
            @PathVariable IntegrationType service,
            @RequestBody IntegrationTemplateRequest request) {
        IntegrationTemplateResponse response = adminService.updateIntegration(service, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{service}/toggle")
    public ResponseEntity<IntegrationTemplateResponse> toggleActiveStatus(@PathVariable IntegrationType service) {
        IntegrationTemplateResponse response = adminService.toggleActiveStatus(service);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<IntegrationTemplateResponse>> getAllIntegrations() {
        return ResponseEntity.ok(adminService.getAllIntegrations());
    }

    @GetMapping("/{service}")
    public ResponseEntity<IntegrationTemplateResponse> getIntegrationByService(@PathVariable IntegrationType service) {
        return ResponseEntity.ok(adminService.getIntegrationByService(service));
    }

    @DeleteMapping("/{service}")
    public ResponseEntity<Void> softDeleteIntegration(@PathVariable IntegrationType service) {
        adminService.softDeleteIntegration(service);
        return ResponseEntity.noContent().build();
    }
}
