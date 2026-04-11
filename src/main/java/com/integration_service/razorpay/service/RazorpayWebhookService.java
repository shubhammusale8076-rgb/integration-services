package com.integration_service.razorpay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.integration_service.config.TenantContext;
import com.integration_service.constants.EventTypes;
import com.integration_service.dto.EventRequest;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.integration.parser.ParserFactory;
import com.integration_service.integration.parser.WebhookParser;
import com.integration_service.razorpay.RazorpayConfig.RazorpayConfig;
import com.integration_service.repository.IntegrationTemplateRepo;
import com.integration_service.service.EventService;
import com.integration_service.service.ExecutionLogService;
import com.integration_service.service.GymCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final ObjectMapper objectMapper;
    private final IntegrationTemplateRepo configRepository;
    private final EventService eventService;
    private final ExecutionLogService logService;
    private final GymCallbackService gymCallbackService;
    private final ParserFactory parserFactory;
    private final List<IntegrationHandler> handlers;

    public void processWebhook(String signature, String payload) {

        try {
            JsonNode json = objectMapper.readTree(payload);

            WebhookParser parser = parserFactory.getParser("RAZORPAY");

            // extract tenantId (IMPORTANT DESIGN DECISION)
            String tenantId = parser.extractTenantId(json);

            TenantContext.setTenant(tenantId);

            IntegrationTemplate config = configRepository
                    .findByTenantIdAndService(tenantId, "RAZORPAY")
                    .orElseThrow(() -> new RuntimeException("Config not found"));

            IntegrationHandler handler = handlers.stream()
                    .filter(h -> "RAZORPAY".equalsIgnoreCase(h.getService()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Razorpay handler not found"));

            RazorpayConfig razorpayConfig = handler.parseConfig(config, RazorpayConfig.class);



            // 🔐 verify signature
            if (!verifySignature(payload, signature, razorpayConfig.getWebhookSecret())) {
                throw new RuntimeException("Invalid signature");
            }

            // ✅ handle event
            handleEvent(parser, json);

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


    private void handleEvent(WebhookParser parser, JsonNode json) {

        String eventType = parser.extractEventType(json);

        if ("payment.captured".equals(eventType)) {

            JsonNode payment = json.get("payload")
                    .get("payment")
                    .get("entity");

            String phone = payment.has("contact") ? payment.get("contact").asText() : null;

            String name = payment.has("notes") && payment.get("notes").has("name")
                    ? payment.get("notes").get("name").asText()
                    : "Member";

            int amount = payment.get("amount").asInt();

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("paymentId", payment.get("id").asText());
            eventData.put("amount", amount);
            eventData.put("phone", phone);
            eventData.put("name", name);

            // 🔥 1. Trigger internal event
            EventRequest event = new EventRequest();
            event.setEventType(EventTypes.PAYMENT_SUCCESS);
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
