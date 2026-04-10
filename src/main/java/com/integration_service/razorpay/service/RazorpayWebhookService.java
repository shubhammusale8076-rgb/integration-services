package com.integration_service.razorpay.service;

import com.integration_service.config.TenantContext;
import com.integration_service.dto.EventRequest;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.razorpay.RazorpayConfig.RazorpayConfig;
import com.integration_service.repository.IntegrationTemplateRepo;
import com.integration_service.service.EventService;
import com.integration_service.service.ExecutionLogService;
import com.integration_service.service.GymCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final ObjectMapper objectMapper;
    private final IntegrationTemplateRepo configRepository;
    private final EventService eventService;
    private final ExecutionLogService logService;
    private final GymCallbackService gymCallbackService;

    public void processWebhook(String signature, String payload) {

        try {
            JsonNode json = objectMapper.readTree(payload);

            String eventType = json.get("event").asText();

            // extract tenantId (IMPORTANT DESIGN DECISION)
            String tenantId = extractTenant(json);

            TenantContext.setTenant(tenantId);

            IntegrationTemplate config = configRepository
                    .findByTenantIdAndService(tenantId, "RAZORPAY")
                    .orElseThrow(() -> new RuntimeException("Config not found"));

            RazorpayConfig razorpayConfig = objectMapper.readValue(
                    config.getConfigSchema(),
                    RazorpayConfig.class
            );

            // 🔐 verify signature
            if (!verifySignature(payload, signature, razorpayConfig.getWebhookSecret())) {
                throw new RuntimeException("Invalid signature");
            }

            // ✅ handle event
            handleEvent(eventType, json);

        } catch (Exception ex) {
            logService.logFailure("RAZORPAY", "WEBHOOK", payload, ex);
        } finally {
            TenantContext.clear();
        }
    }

    public boolean verifySignature(String payload, String actualSignature, String secret) {
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

    private String extractTenant(JsonNode json) {
        return json.get("payload")
                .get("payment")
                .get("entity")
                .get("notes")
                .get("tenantId")
                .asText();
    }

    private void handleEvent(String eventType, JsonNode json) {

        if ("payment.captured".equals(eventType)) {

            JsonNode payment = json.get("payload")
                    .get("payment")
                    .get("entity");

            Map<String, Object> eventData = Map.of(
                    "paymentId", payment.get("id").asText(),
                    "amount", payment.get("amount").asInt(),
                    "status", payment.get("status").asText(),
                    "phone", payment.get("contact").asText()
            );

            // 🔥 1. Trigger internal event
            EventRequest event = new EventRequest();
            event.setEventType("PAYMENT_SUCCESS");
            event.setData(eventData);

            eventService.processEvent(event);

            // 🔥 2. Notify Gym App
            gymCallbackService.notifyPaymentSuccess(eventData);

            // 🔥 3. Log success
            logService.logSuccess("RAZORPAY", "PAYMENT_SUCCESS", eventData, "Webhook processed");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String s = Integer.toHexString(0xff & b);
            if (s.length() == 1) hex.append('0');
            hex.append(s);
        }
        return hex.toString();
    }
}
