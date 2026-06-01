package com.integration_service.integrations.whatsapp.controller;

import com.integration_service.integrations.whatsapp.service.WhatsAppTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/whatsapp/templates")
@RequiredArgsConstructor
public class WhatsAppTemplateController {

    private final WhatsAppTemplateService whatsAppTemplateService;

    @PostMapping("/sync/{tenantId}")
    public ResponseEntity<String> syncTemplates(@PathVariable UUID tenantId) {

        whatsAppTemplateService.syncTemplates(tenantId);

        return ResponseEntity.ok("Templates synced successfully.");
    }

    @PostMapping("/provision-template/{tenantId}")
    public ResponseEntity<String> provisionTemplates(@PathVariable UUID tenantId){

        whatsAppTemplateService.provisionTemplates(tenantId);

        return ResponseEntity.ok("Templates Provision Started successfully.");
    }

}
