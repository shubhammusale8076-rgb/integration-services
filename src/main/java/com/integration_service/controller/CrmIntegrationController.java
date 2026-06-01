package com.integration_service.controller;

import com.integration_service.dto.ResponseDto;
import com.integration_service.dto.WhatsAppMessageRequest;
import com.integration_service.integrations.google.calendar.service.CrmCalendarService;
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
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class CrmIntegrationController {

    private final WhatsAppMessagingService whatsappService;
    private final CrmCalendarService calendarService;

    @Value("${internal.api.secret}")
    private String internalSecret;

    @PostMapping("/whatsapp/send")
    public ResponseEntity<ResponseDto> sendWhatsApp(
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestBody WhatsAppMessageRequest request) {

        validateSecret(secret);

        String result = whatsappService.sendTemplateMessage(request);
        return ResponseEntity.ok(ResponseDto.builder()
                .code(200)
                .message(result)
                .build());
    }

    @PostMapping("/google/calendar/events")
    public ResponseEntity<ResponseDto> createCalendarEvent(
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody Map<String, Object> data) {

        validateSecret(secret);

        String eventId = calendarService.createTrialBookingEvent(tenantId, data);
        return ResponseEntity.ok(ResponseDto.builder()
                .code(200)
                .message("Event created with ID: " + eventId)
                .build());
    }

    private void validateSecret(String secret) {
        if (!internalSecret.equals(secret)) {
            throw new RuntimeException("Unauthorized internal access");
        }
    }
}
