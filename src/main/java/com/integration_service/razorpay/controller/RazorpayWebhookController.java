package com.integration_service.razorpay.controller;

import com.integration_service.razorpay.service.RazorpayWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final ObjectMapper objectMapper;

    private final RazorpayWebhookService webhookService;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody String payload) {

        // TODO: verify signature

        webhookService.processWebhook(signature, payload);

        // parse event
        // send to gym app

        return ResponseEntity.ok("OK");
    }
}
