package com.integration_service.integrations.parser;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface WebhookParser {
    String getSource();
    String extractTenantId(JsonNode payload);
    String extractEventType(JsonNode payload);
    Map<String, Object> parsePayload(JsonNode payload);
}

