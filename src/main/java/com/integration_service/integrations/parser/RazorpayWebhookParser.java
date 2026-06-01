package com.integration_service.integrations.parser;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RazorpayWebhookParser implements WebhookParser {

    @Override
    public String getSource() {
        return "RAZORPAY";
    }

    @Override
    public String extractTenantId(JsonNode json) {
        JsonNode notes = extractNotes(json);
        if (!notes.isMissingNode() && notes.has("tenantId")) {
            return notes.get("tenantId").asText();
        }
        throw new IllegalArgumentException("Missing tenantId in Razorpay webhook payload notes");
    }

    @Override
    public String extractEventType(JsonNode json) {
        return json.get("event").asText();
    }

    @Override
    public Map<String, Object> parsePayload(JsonNode json) {
        String eventType = extractEventType(json);
        JsonNode entity = extractPrimaryEntity(json, eventType);

        if (entity.isMissingNode()) {
            return Map.of("eventType", eventType);
        }

        return Map.of(
                "eventType", eventType,
                "paymentId", textOrEmpty(entity, "id"),
                "paymentLinkId", textOrEmpty(entity, "payment_link_id"),
                "amount", entity.path("amount").asInt(0),
                "status", textOrEmpty(entity, "status"),
                "phone", textOrEmpty(entity, "contact")
        );
    }

    public JsonNode extractNotes(JsonNode json) {
        JsonNode fromPayment = json.at("/payload/payment/entity/notes");
        if (!fromPayment.isMissingNode() && !fromPayment.isEmpty()) {
            return fromPayment;
        }
        JsonNode fromLink = json.at("/payload/payment_link/entity/notes");
        if (!fromLink.isMissingNode() && !fromLink.isEmpty()) {
            return fromLink;
        }
        return json.at("/payload/payment_link/entity/notes");
    }

    public JsonNode extractPrimaryEntity(JsonNode json, String eventType) {
        if (eventType != null && eventType.startsWith("payment_link.")) {
            return json.at("/payload/payment_link/entity");
        }
        return json.at("/payload/payment/entity");
    }

    public String extractOrderId(
            JsonNode json,
            String eventType
    ) {

        try {

            // =====================================================
            // payment.captured / payment.failed
            // =====================================================

            JsonNode paymentEntity = json.at("/payload/payment/entity");

            if (!paymentEntity.isMissingNode() && paymentEntity.has("order_id")) {

                String orderId = paymentEntity.get("order_id").asText();

                if (orderId != null && !orderId.isBlank()) {
                    return orderId;
                }
            }

            // =====================================================
            // order.paid fallback
            // =====================================================

            JsonNode orderEntity = json.at("/payload/order/entity");

            if (!orderEntity.isMissingNode() && orderEntity.has("id")) {

                String orderId = orderEntity.get("id").asText();

                if (orderId != null && !orderId.isBlank()) {
                    return orderId;
                }
            }

            log.warn("Unable to extract Razorpay order id from webhook: eventType={}", eventType);

            return null;

        } catch (Exception ex) {

            log.error("Failed to extract Razorpay order id: eventType={}, error={}", eventType, ex.getMessage(), ex);
            return null;
        }
    }

    public String extractPaymentId(JsonNode json, String eventType) {
        if ("payment_link.paid".equals(eventType)) {
            JsonNode payments = json.at("/payload/payment_link/entity/payments");
            if (payments.isArray() && !payments.isEmpty()) {
                return textOrNull(payments.get(0), "payment_id");
            }
        }
        return textOrNull(json.at("/payload/payment/entity"), "id");
    }

    private static String textOrEmpty(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || !node.has(field)) {
            return null;
        }
        String value = node.get(field).asText();
        return value.isBlank() ? null : value;
    }
}
