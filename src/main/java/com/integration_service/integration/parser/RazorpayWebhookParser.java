package com.integration_service.integration.parser;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RazorpayWebhookParser implements WebhookParser {

    @Override
    public String getSource() {
        return "RAZORPAY";
    }

    @Override
    public String extractTenantId(JsonNode json) {
        return json.get("payload")
                .get("payment")
                .get("entity")
                .get("notes")
                .get("tenantId")
                .asText();
    }

    @Override
    public String extractEventType(JsonNode json) {
        return json.get("event").asText();
    }

    public String extractExternalEventId(JsonNode json) {
        return json.get("id").asText();
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
