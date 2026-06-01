package com.integration_service.integrations.razorpay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.common.config.CorrelationContext;
import com.integration_service.communication.dto.IntegrationHealthResult;
import com.integration_service.communication.dto.IntegrationValidationRequest;
import com.integration_service.communication.dto.IntegrationValidationResponse;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.dto.EventRequest;
import com.integration_service.handler.HealthCheckSupport;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.handler.IntegrationHandlerHealthContext;
import com.integration_service.integrations.razorpay.RazorpayConfig.RazorpayConfig;
import com.integration_service.integrations.razorpay.dto.RazorpayOrderResult;
import com.integration_service.integrations.razorpay.dto.RazorpayPaymentLinkResult;
import com.integration_service.integrations.razorpay.service.RazorpayClientService;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RazorpayHandler implements IntegrationHandler {

    private final ObjectMapper objectMapper;
    private final RazorpayClientService razorpayClientService;

    @Value("${app.public-base-url:http://localhost:8082}")
    private String publicBaseUrl;

    @Override
    public IntegrationType getService() {
        return IntegrationType.RAZORPAY;
    }

    @Override
    public void validate(Map<String, Object> config) throws Exception {
        validateCredentials(resolveKeyId(config), resolveKeySecret(config));
    }

    @Override
    public void connect(Map<String, Object> config) throws Exception {
        log.info("Razorpay connected successfully");
    }

    @Override
    public IntegrationHealthResult validateHealth(TenantIntegration integration) {
        try {
            RazorpayConfig config = parseConfig(integration, RazorpayConfig.class);
            String keyId = firstNonBlank(config.getKey(), readMetadataField(integration, "keyId"));
            String keySecret = firstNonBlank(config.getKeySecret(), readMetadataField(integration, "keySecret"));

            if (keyId == null || keySecret == null) {
                return HealthCheckSupport.failed("Razorpay credentials are missing");
            }

            validateCredentials(keyId, keySecret);
            IntegrationHandlerHealthContext.setLastError(null);
            return HealthCheckSupport.healthy();
        } catch (Exception ex) {
            log.warn("Razorpay health check failed for tenant {}: {}",
                    integration.getTenantId(), ex.getMessage());
            return HealthCheckSupport.fromThrowable(ex);
        }
    }

    @Override
    public boolean supports(String eventType) {
        return "PAYMENT_CREATE".equals(eventType)
                || "MANUAL_TRIGGER".equals(eventType)
                || "PAYMENT_SUCCESS".equals(eventType);
    }

    @Override
    public Object execute(EventRequest event, TenantIntegration config) {
        try {
            if ("MANUAL_TRIGGER".equals(event.getEventType())) {
                validateManualPayload(event.getData());
            }

            RazorpayConfig razorpayConfig = parseConfig(config, RazorpayConfig.class);

            Map<String, Object> data = event.getData();

            double amountRupees = ((Number) data.get("amount")).doubleValue();
            String phone = (String) data.get("phone");

            UUID memberId = config.getTenantId();
            if (data.get("memberId") != null) {
                memberId = UUID.fromString(String.valueOf(data.get("memberId")));
            }

            RazorpayClientService.PaymentLinkContext context = new RazorpayClientService.PaymentLinkContext(
                    config.getTenantId(),
                    memberId,
                    null,
                    null,
                    CorrelationContext.get(),
                    (String) data.get("name"),
                    (String) data.get("email"),
                    phone,
                    "Manual payment link"
            );

            RazorpayOrderResult link = razorpayClientService.createPaymentLink(
                    razorpayConfig,
                    amountRupees,
                    context
            );

            return Map.of("paymentLink", link.getUniversalPaymentLink());

        } catch (Exception e) {
            throw new RuntimeException("Razorpay execution failed", e);
        }
    }

    @Override
    public <T> T parseConfig(TenantIntegration integration, Class<T> clazz) {
        try {
            return objectMapper.readValue(integration.getMetadata(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Razorpay config", e);
        }
    }

    private void validateCredentials(String keyId, String keySecret) throws Exception {
        if (keyId == null || keySecret == null) {
            throw new IllegalArgumentException("Razorpay keyId and keySecret are required");
        }
        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);
        razorpayClient.orders.fetchAll(null);
        log.info("Razorpay credentials validated successfully");
    }

    private String resolveKeyId(Map<String, Object> config) {
        return (String) config.get("keyId");
    }

    private String resolveKeySecret(Map<String, Object> config) {
        return (String) config.get("keySecret");
    }

    private String readMetadataField(TenantIntegration integration, String field) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(integration.getMetadata(), Map.class);
            Object value = map.get(field);
            return value != null ? String.valueOf(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private void validateManualPayload(Map<String, Object> data) {
        if (!data.containsKey("amount")) {
            throw new RuntimeException("Amount is required");
        }
        if (!data.containsKey("phone")) {
            throw new RuntimeException("Phone is required");
        }
    }

    @Override
    public IntegrationValidationResponse validateIntegration(IntegrationValidationRequest request) {

        try {
            Map<String, Object> config = request.getConfig();

            String keyId = resolveKeyId(config);
            String keySecret = resolveKeySecret(config);

            String webhookSecret = (String) config.get("webhookSecret");

            // =====================================================
            // 1. Validate Credentials
            // =====================================================

            validateCredentials(keyId, keySecret);

            // =====================================================
            // 2. Validate Webhook Secret
            // =====================================================

            if (webhookSecret == null || webhookSecret.isBlank()) {

                throw new RuntimeException(
                        "Webhook secret is required");
            }

            // =====================================================
            // 3. Generate Webhook URL
            // =====================================================

            String webhookUrl = publicBaseUrl + "/api/webhooks/razorpay";

            // =====================================================
            // 4. Return Detailed Validation
            // =====================================================

            return IntegrationValidationResponse.builder()
                    .success(true)
                    .message("Razorpay validation successful")
                    .checks(Map.of(
                            "api", true,
                            "webhook", true,
                            "webhookUrl", true
                    ))
                    .metadata(Map.of(
                            "webhookUrl", webhookUrl
                    ))
                    .build();

        } catch (Exception ex) {

            log.error(
                    "Razorpay validation failed: {}",
                    ex.getMessage()
            );

            return IntegrationValidationResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build();
        }
    }
}
