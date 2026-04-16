package com.integration_service.integrations.razorpay;

import com.integration_service.dto.EventRequest;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.integrations.razorpay.RazorpayConfig.RazorpayConfig;
import com.integration_service.integrations.razorpay.service.RazorpayClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RazorpayHandler implements IntegrationHandler {

    private final ObjectMapper objectMapper;
    private final RazorpayClientService razorpayService;

    @Override
    public boolean supports(String eventType) {
        return "PAYMENT_CREATE".equals(eventType)
                || "MANUAL_TRIGGER".equals(eventType)
                || "PAYMENT_SUCCESS".equals(eventType);
    }

    @Override
    public Object execute(EventRequest event , IntegrationTemplate config) {

        // TODO: use your existing Razorpay logic here

        try {
            RazorpayConfig razorpayConfig = parseConfig(config, RazorpayConfig.class);

            Map<String, Object> data = event.getData();

            int amount = ((Number) data.get("amount")).intValue();
            String phone = (String) data.get("phone");

            String paymentLink = razorpayService.createPaymentLink(
                    razorpayConfig,
                    amount,
                    phone
            );

            return Map.of(
                    "paymentLink", paymentLink
            );

        } catch (Exception e) {
            throw new RuntimeException("Razorpay execution failed", e);
        }
    }

    @Override
    public String getService() {
        return "RAZORPAY";
    }

    @Override
    public <T> T parseConfig(IntegrationTemplate template, Class<T> clazz) {
        try {
            return objectMapper.readValue(template.getConfigSchema(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Razorpay config", e);
        }
    }
}
