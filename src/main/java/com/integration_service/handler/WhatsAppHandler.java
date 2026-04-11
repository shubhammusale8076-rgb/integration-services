package com.integration_service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.dto.EventRequest;
import com.integration_service.dto.WhatsAppConfig;
import com.integration_service.entity.IntegrationTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WhatsAppHandler implements IntegrationHandler {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String eventType) {
        return "SEND_WHATSAPP".equals(eventType);
    }

    @Override
    public Object execute(EventRequest event, IntegrationTemplate config) {
        WhatsAppConfig whatsAppConfig = parseConfig(config, WhatsAppConfig.class);

        // Logic to call WhatsApp Cloud API
        System.out.println("Sending WhatsApp message using token: " + whatsAppConfig.getAccessToken());

        return Map.of("status", "sent", "to", event.getData().get("to"));
    }

    @Override
    public String getService() {
        return "WHATSAPP";
    }

    @Override
    public <T> T parseConfig(IntegrationTemplate template, Class<T> clazz) {
        try {
            return objectMapper.readValue(template.getConfigSchema(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse WhatsApp config", e);
        }
    }
}
