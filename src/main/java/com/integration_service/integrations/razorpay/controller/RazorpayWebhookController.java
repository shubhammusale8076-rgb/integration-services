package com.integration_service.integrations.razorpay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.common.exceptionHandler.DuplicateWebhookException;
import com.integration_service.common.exceptionHandler.InvalidSignatureException;
import com.integration_service.integrations.razorpay.service.RazorpayWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final ObjectMapper objectMapper;

    private final RazorpayWebhookService webhookService;

    @PostMapping("/webhooks/razorpay")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String payload) {

        try {

            webhookService.processWebhook(signature, payload);

            return ResponseEntity.ok().build();

        } catch (InvalidSignatureException e) {


            // Razorpay should NOT retry invalid signatures
            return ResponseEntity.badRequest().build();

        } catch (DuplicateWebhookException e) {

            log.info("Duplicate webhook ignored: {}", e.getMessage());

            return ResponseEntity.ok().build();

        } catch (Exception e) {


            // Return 200 to avoid infinite Razorpay retries
            // Internal retry should happen via outbox/retry mechanism
            return ResponseEntity.ok().build();
        }
    }
}
