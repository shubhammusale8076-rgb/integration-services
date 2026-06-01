package com.integration_service.controller;

import com.integration_service.common.config.TenantContext;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.dto.integrationDto.IntegrationCardResponse;
import com.integration_service.dto.integrationDto.IntegrationCatalogResponse;
import com.integration_service.service.IntegrationCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationCatalogController {

    private final IntegrationCatalogService catalogService;

    @GetMapping("/catalog")
    public ResponseEntity<List<IntegrationCardResponse>> getCatalog() {
        String tenantStr = TenantContext.getTenant();
        if (tenantStr == null) {
            return ResponseEntity.badRequest().build();
        }
        UUID tenantId = UUID.fromString(tenantStr);
        List<IntegrationCardResponse> catalog = catalogService.getCatalog(tenantId);
        return ResponseEntity.ok(catalog);
    }

    @GetMapping("/service/{integrationType}")
    public ResponseEntity<IntegrationCatalogResponse> getDetails(@PathVariable IntegrationType integrationType) {

        UUID tenantId = UUID.fromString(TenantContext.getTenant());

        return ResponseEntity.ok(catalogService.getDetails(tenantId, integrationType));
    }
}
