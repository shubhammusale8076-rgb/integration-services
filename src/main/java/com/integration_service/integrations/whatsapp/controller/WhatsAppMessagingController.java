package com.integration_service.integrations.whatsapp.controller;

import com.integration_service.common.constants.SecurityConstants;
import com.integration_service.communication.entity.WhatsAppDeliveryStatus;
import com.integration_service.dto.WhatsAppMessageRequest;
import com.integration_service.integrations.whatsapp.dto.WelcomeMessage;
import com.integration_service.integrations.google.dto.GooglePasswordResetRequestDto;
import com.integration_service.integrations.whatsapp.service.WhatsAppMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/internal/whatsapp")
@RequiredArgsConstructor
public class WhatsAppMessagingController {

    private final WhatsAppMessagingService messagingService;

    @Value("${internal.api.secret}")
    private String internalSecret;

    @PostMapping("/send")
    public ResponseEntity<String> sendTemplateMessage(
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestBody WhatsAppMessageRequest request) {

        if (!internalSecret.equals(secret)) {
            log.warn("Unauthorized internal access attempt with secret: {}", secret);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid secret");
        }

        try {
            String result = messagingService.sendTemplateMessage(request);
            if ("SUCCESS".equals(result)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            log.error("Error sending WhatsApp template message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/welcome-whatsapp")
    public ResponseEntity<Map<String, WhatsAppDeliveryStatus>> sendWelcomeWhatsApp(
            @RequestHeader(value = SecurityConstants.HEADER_CORRELATION_ID, required = false) String correlationId,
            @RequestBody WelcomeMessage request) {

        if (correlationId != null) {
            request.setCorrelationId(correlationId);
        }
        try {
            WhatsAppDeliveryStatus status =  messagingService.sendWelcomeWhatsApp(request);

            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            log.error("Welcome WhatsApp failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", WhatsAppDeliveryStatus.FAILED));
        }
    }



}
