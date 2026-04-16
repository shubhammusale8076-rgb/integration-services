package com.integration_service.integrations.parser;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

@Component
public class RazorpayWebhookParser implements WebhookParser {

    @Override
    public String getSource() {
        return "RAZORPAY";
    }

    @Override
    public String extractTenantId(JsonNode json) {
        JsonNode notes = json.at("/payload/payment/entity/notes");
        if (notes.isMissingNode() || !notes.has("tenantId")) {
            throw new IllegalArgumentException("Missing tenantId in webhook payload");
        }
        return notes.get("tenantId").asText();
    }

    @Override
    public String extractEventType(JsonNode json) {
        return json.get("event").asText();
    }

    @Override
    public Map<String, Object> parsePayload(JsonNode json) {
        String eventType = extractEventType(json);

        if ("payment.captured".equals(eventType)) {
            JsonNode payment = json.get("payload")
                    .get("payment")
                    .get("entity");

            return Map.of(
                    "paymentId", payment.get("id").asText(),
                    "amount", payment.get("amount").asInt(),
                    "status", payment.get("status").asText(),
                    "phone", payment.get("contact").asText()
            );
        }

        return Map.of();
    }
}

