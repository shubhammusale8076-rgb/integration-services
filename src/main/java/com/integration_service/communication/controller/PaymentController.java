package com.integration_service.communication.controller;

import com.integration_service.communication.dto.*;
import com.integration_service.communication.service.PaymentService;
import com.integration_service.common.config.CorrelationContext;
import com.integration_service.common.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-link")
    public ResponseEntity<PaymentLinkResponse> createPaymentLink(
            @RequestHeader(SecurityConstants.HEADER_INTERNAL_SECRET) String secret,
            @RequestHeader(value = SecurityConstants.HEADER_CORRELATION_ID, required = false) String correlationId,
            @RequestBody PaymentLinkRequest request) {

        applyCorrelation(correlationId, request);

        try {
            PaymentLinkResponse response = paymentService.createPaymentLink(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating payment link, correlationId={}: {}",
                    CorrelationContext.get(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/access/{token}")
    public ResponseEntity<PaymentAccessInternalResponse> getPaymentAccess(@PathVariable String token) {

        PaymentAccessInternalResponse response= paymentService.getPaymentAccess(token);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/{transactionId}/resend-whatsapp")
    public ResponseEntity<PaymentLinkResponse> resendWhatsApp(
            @PathVariable UUID transactionId,
            @RequestHeader(value = SecurityConstants.HEADER_CORRELATION_ID, required = false) String correlationId) {

        if (correlationId != null) {
            CorrelationContext.set(correlationId);
        }
        try {
            return ResponseEntity.ok(paymentService.resendPaymentLinkWhatsApp(transactionId));
        } catch (Exception e) {
            log.error("Error resending WhatsApp for transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } finally {
            CorrelationContext.clear();
        }
    }




    @PostMapping("/receipt-email")
    public ResponseEntity<Map<String, String>> sendReceiptEmail(
            @RequestHeader(value = SecurityConstants.HEADER_CORRELATION_ID, required = false) String correlationId,
            @RequestBody PaymentReceiptEmailRequest request) {

        if (correlationId != null) {
            request.setCorrelationId(correlationId);
        }
        try {
            paymentService.sendPaymentReceiptEmail(request);
            return ResponseEntity.ok(Map.of("status", "SENT"));
        } catch (Exception e) {
            log.error("Receipt email failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "FAILED", "message", e.getMessage()));
        }
    }

    private void applyCorrelation(String headerCorrelationId, PaymentLinkRequest request) {
        if (headerCorrelationId != null && !headerCorrelationId.isBlank()) {
            CorrelationContext.set(headerCorrelationId);
            if (request.getCorrelationId() == null) {
                request.setCorrelationId(headerCorrelationId);
            }
        } else if (request.getCorrelationId() != null) {
            CorrelationContext.set(request.getCorrelationId());
        }
    }
}
