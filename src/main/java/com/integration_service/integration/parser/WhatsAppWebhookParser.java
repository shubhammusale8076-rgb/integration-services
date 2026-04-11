package com.integration_service.integration.parser;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WhatsAppWebhookParser implements WebhookParser {

    @Override
    public String getSource() {
        return "WHATSAPP";
    }

    @Override
    public String extractTenantId(JsonNode json) {
        // Implementation for WhatsApp Cloud API
        // Usually, you might need a way to map the sender's phone number or a reference to a tenant
        // For simplicity, we assume there's a custom field or a predefined mapping
        return "default-tenant";
    }

    @Override
    public String extractEventType(JsonNode json) {
        return "WHATSAPP_MESSAGE";
    }

    @Override
    public String extractExternalEventId(JsonNode json) {
        return json.at("/entry/0/changes/0/value/messages/0/id").asText();
    }

    @Override
    public Map<String, Object> parsePayload(JsonNode json) {
        JsonNode message = json.at("/entry/0/changes/0/value/messages/0");
        return Map.of(
            "from", message.get("from").asText(),
            "text", message.at("/text/body").asText()
        );
    }
}
