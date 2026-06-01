package com.integration_service.integrations.razorpay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.common.config.CorrelationContext;
import com.integration_service.common.config.TenantContext;
import com.integration_service.common.exceptionHandler.DuplicateWebhookException;
import com.integration_service.common.exceptionHandler.InvalidSignatureException;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.PaymentTransaction;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.entity.TransactionStatus;
import com.integration_service.communication.repository.PaymentTransactionRepository;
import com.integration_service.communication.service.PaymentCoreSyncService;
import com.integration_service.entity.EventStatus;
import com.integration_service.entity.WebhookEvent;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.integrations.parser.ParserFactory;
import com.integration_service.integrations.parser.RazorpayWebhookParser;
import com.integration_service.integrations.parser.WebhookParser;
import com.integration_service.integrations.razorpay.RazorpayConfig.RazorpayConfig;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.repository.WebhookEventRepo;
import com.integration_service.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final ObjectMapper objectMapper;
    private final TenantIntegrationRepository tenantIntegrationRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WebhookEventRepo webhookEventRepo;
    private final ExecutionLogService logService;
    private final ParserFactory parserFactory;
    private final List<IntegrationHandler> handlers;
    private final PaymentCoreSyncService paymentCoreSyncService;
    private final RazorpayWebhookParser razorpayWebhookParser;

    public void processWebhook(String signature, String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            WebhookParser parser = parserFactory.getParser("RAZORPAY");

            String tenantId = parser.extractTenantId(json);
            TenantContext.setTenant(tenantId);

            JsonNode notes = razorpayWebhookParser.extractNotes(json);
            String correlationId = notes.has("correlationId") ? notes.get("correlationId").asText() : null;
            if (correlationId != null && !correlationId.isBlank()) {
                CorrelationContext.set(correlationId);
            }

            TenantIntegration config = tenantIntegrationRepository
                    .findByTenantIdAndIntegrationType(UUID.fromString(tenantId), IntegrationType.RAZORPAY)
                    .orElseThrow(() -> new RuntimeException("Razorpay config not found"));

            IntegrationHandler handler = handlers.stream()
                    .filter(h -> IntegrationType.RAZORPAY.equals(h.getService()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Razorpay handler not found"));

            RazorpayConfig razorpayConfig = handler.parseConfig(config, RazorpayConfig.class);

            if (!verifySignature(payload, signature, razorpayConfig.getWebhookSecret())) {
                throw new InvalidSignatureException("Invalid Razorpay webhook signature");
            }

            handleEvent(parser, json, payload);

        } catch (JsonProcessingException ex) {
            log.error("Invalid Razorpay webhook JSON: {}", ex.getMessage());
            throw new RuntimeException("Invalid webhook payload", ex);
        } finally {
            TenantContext.clear();
            CorrelationContext.clear();
        }
    }

    private void handleEvent(WebhookParser parser, JsonNode json, String payload) {
        String eventType = parser.extractEventType(json);

        String eventId = json.has("event_id") ? json.get("event_id").asText() : null;
        if (eventId == null || eventId.isBlank()) {
            throw new RuntimeException("Missing Razorpay event ID");
        }

        log.info("Processing Razorpay webhook: eventType={}, eventId={}, correlationId={}",
                eventType, eventId, CorrelationContext.get());

        if (webhookEventRepo.existsByExternalEventId(eventId)) {
            log.info("Webhook already processed (idempotent skip): {}", eventId);
            throw new DuplicateWebhookException("Webhook already processed: " + eventId);
        }

        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setTenantId(TenantContext.getTenant());
        webhookEvent.setSource("RAZORPAY");
        webhookEvent.setEventType(eventType);
        webhookEvent.setExternalEventId(eventId);
        webhookEvent.setPayload(payload);
        webhookEvent.setStatus(EventStatus.PROCESSING);
        webhookEvent.setCreatedAt(LocalDateTime.now());
        webhookEvent = webhookEventRepo.save(webhookEvent);

        try {
            switch (eventType) {
                case "payment_link.paid", "payment.captured" -> processPaymentSuccess(json, eventType);
                case "payment.failed" -> processPaymentFailed(json);
                default -> log.info("Ignoring unsupported Razorpay event type: {}", eventType);
            }

            webhookEvent.setStatus(EventStatus.DONE);
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepo.save(webhookEvent);

            logService.logSuccess(IntegrationType.RAZORPAY, "WEBHOOK_" + eventType, payload, "processed");

        } catch (Exception ex) {
            webhookEvent.setStatus(EventStatus.FAILED);
            webhookEvent.setRetryCount(webhookEvent.getRetryCount() != null ? webhookEvent.getRetryCount() + 1 : 1);
            webhookEventRepo.save(webhookEvent);
            logService.logFailure(IntegrationType.RAZORPAY, "WEBHOOK_" + eventType, payload, ex);
            throw ex;
        }
    }

    private void processPaymentSuccess(JsonNode json, String eventType) {
        String razorpayOrderId = razorpayWebhookParser.extractOrderId(json, eventType);
        String paymentId = razorpayWebhookParser.extractPaymentId(json, eventType);
        JsonNode entity = razorpayWebhookParser.extractPrimaryEntity(json, eventType);
        int amountPaise = entity.path("amount").asInt(0);

        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {

            throw new RuntimeException("razorpay_order_id missing in webhook");
        }

        PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found for order: " + razorpayOrderId));

        if (transaction.getCorrelationId() != null) {
            CorrelationContext.set(transaction.getCorrelationId());
        }

        log.info("Payment success webhook: orderId={}, paymentId={}, correlationId={}",
                razorpayOrderId, paymentId, transaction.getCorrelationId());

        if (transaction.getWebhookReceived() != null
                && transaction.getWebhookReceived()
                && transaction.getStatus() == TransactionStatus.SUCCESS) {

            log.info(
                    "Webhook already processed for transaction={}",
                    transaction.getId()
            );

            return;
        }

        paymentCoreSyncService.markPaymentPaid(transaction, paymentId, amountPaise);

        boolean synced = paymentCoreSyncService.syncPaymentToCore(transaction);

        if (!synced) {
            log.warn("Core sync pending after payment success: transactionId={}", transaction.getId());
        }
    }

    private void processPaymentFailed(JsonNode json) {
        String razorpayOrderId = razorpayWebhookParser.extractOrderId(json, "payment.failed");
        JsonNode payment = json.at("/payload/payment/entity");
        String paymentId = payment.has("id") ? payment.get("id").asText() : null;

        if (razorpayOrderId == null) {

            log.warn("payment.failed without order id, paymentId={}", paymentId);

            return;
        }

        paymentTransactionRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(transaction -> {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setRazorpayPaymentId(paymentId);
            transaction.setWebhookReceived(true);
            transaction.setLastError("Payment failed at Razorpay");
            paymentTransactionRepository.save(transaction);
            log.info("Marked payment transaction failed: {}", transaction.getId());
        });
    }

    public boolean verifySignature(String payload, String actualSignature, String secret) {
        if (actualSignature == null || secret == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes());
            String generatedSignature = bytesToHex(hash);

            return MessageDigest.isEqual(
                    generatedSignature.getBytes(),
                    actualSignature.getBytes()
            );
        } catch (Exception e) {
            throw new RuntimeException("Signature verification failed", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String s = Integer.toHexString(0xff & b);
            if (s.length() == 1) {
                hex.append('0');
            }
            hex.append(s);
        }
        return hex.toString();
    }
}
