package com.integration_service.controller;

import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.dto.integrationDto.IntegrationConfigResponse;
import com.integration_service.dto.integrationDto.TenantIntegrationRequest;
import com.integration_service.dto.ResponseDto;
import com.integration_service.service.integrationService.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class IntegrationConfigController {

    private final IntegrationConfigService service;

    @PostMapping("/save")
    public ResponseEntity<ResponseDto> save(@RequestBody TenantIntegrationRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<IntegrationConfigResponse>> getAll() {
        return ResponseEntity.ok(service.getTenantConfigs());
    }

    @GetMapping("/{serviceName}")
    public ResponseEntity<IntegrationConfigResponse> getConfig(@PathVariable IntegrationType serviceName) {
        IntegrationConfigResponse response = service.getConfigByService(serviceName);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{serviceName}/toggle")
    public ResponseEntity<?> toggle(@PathVariable IntegrationType serviceName) {
        return ResponseEntity.ok(service.toggle(serviceName));
    }

    @PutMapping("/{serviceName}/mode")
    public ResponseEntity<?> updateMode(@PathVariable IntegrationType serviceName, @RequestParam String mode) {

        return ResponseEntity.ok(service.updateMode(serviceName, mode));
    }
}
