package com.integration_service.controller;

import com.integration_service.integrations.whatsapp.service.MessageLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final MessageLogService logService;

    @PostMapping
    public ResponseEntity<String> handle(@RequestBody Map<String, Object> payload) {

        try {

            List<Map<String, Object>> entry =
                    (List<Map<String, Object>>) payload.get("entry");

            Map<String, Object> changes =
                    (Map<String, Object>) ((List<?>) entry.get(0).get("changes")).get(0);

            Map<String, Object> value =
                    (Map<String, Object>) changes.get("value");

            List<Map<String, Object>> statuses =
                    (List<Map<String, Object>>) value.get("statuses");

            for (Map<String, Object> status : statuses) {

                String messageId = (String) status.get("id");
                String statusValue = (String) status.get("status");

                logService.updateStatus(messageId, statusValue);
            }

        } catch (Exception e) {
            System.err.println("Webhook parsing failed: " + e.getMessage());
        }

        return ResponseEntity.ok("OK");
    }
}
