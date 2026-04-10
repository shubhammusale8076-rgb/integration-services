package com.integration_service.razorpay;

import com.integration_service.dto.EventRequest;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.razorpay.RazorpayConfig.RazorpayConfig;
import com.integration_service.razorpay.service.RazorpayClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RazorpayHandler implements IntegrationHandler {

    private final ObjectMapper objectMapper;
    private final RazorpayClientService razorpayService;

    @Override
    public boolean supports(String eventType) {
        return "PAYMENT_CREATE".equals(eventType)
                || "MANUAL_TRIGGER".equals(eventType);
    }

    @Override
    public Object  execute(EventRequest event , IntegrationTemplate config) {

        // TODO: use your existing Razorpay logic here

        try {
            RazorpayConfig razorpayConfig = objectMapper.readValue(
                    config.getConfigSchema(),
                    RazorpayConfig.class
            );

            Map<String, Object> data = event.getData();

            int amount = (int) data.get("amount");
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
}
