package com.integration_service.integrations.razorpay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.integrations.razorpay.RazorpayConfig.RazorpayConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RazorpayCredentialResolver {

    private final ObjectMapper objectMapper;

    public RazorpayConfig resolve(TenantIntegration integration) {
        try {
            RazorpayConfig config = objectMapper.readValue(integration.getMetadata(), RazorpayConfig.class);
            if (config.getKey() == null || config.getKey().isBlank()) {
                config.setKey(readField(integration, "keyId"));
            }
            if (config.getKeySecret() == null || config.getKeySecret().isBlank()) {
                config.setKeySecret(readField(integration, "keySecret"));
            }
            if (config.getWebhookSecret() == null || config.getWebhookSecret().isBlank()) {
                config.setWebhookSecret(readField(integration, "webhookSecret"));
            }
            return config;
        } catch (Exception e) {
            RazorpayConfig fallback = new RazorpayConfig();
            fallback.setKey(readField(integration, "keyId"));
            fallback.setKeySecret(readField(integration, "keySecret"));
            fallback.setWebhookSecret(readField(integration, "webhookSecret"));
            return fallback;
        }
    }

    private String readField(TenantIntegration integration, String field) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(integration.getMetadata(), Map.class);
            Object value = map.get(field);
            if (value == null) {
                value = map.get("apiKey".equals(field) ? "keyId" : field);
            }
            if (value == null && "keySecret".equals(field)) {
                value = map.get("apiSecret");
            }
            return value != null ? String.valueOf(value) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
