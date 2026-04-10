package com.integration_service.controller;

import com.integration_service.dto.integrationDto.IntegrationSummaryResponse;
import com.integration_service.service.integrationService.IntegrationUIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationUIController {

    private final IntegrationUIService service;

    // ✅ 1. List all integrations (cards)
    @GetMapping
    public ResponseEntity<List<IntegrationSummaryResponse>> getAll() {
        return ResponseEntity.ok(service.getAllIntegrations());
    }
}
