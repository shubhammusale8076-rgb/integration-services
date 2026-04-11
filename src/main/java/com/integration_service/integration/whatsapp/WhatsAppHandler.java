package com.integration_service.integration.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration_service.constants.EventTypes;
import com.integration_service.constants.Services;
import com.integration_service.constants.WhatsAppTemplates;
import com.integration_service.dto.EventRequest;
import com.integration_service.dto.configDto.WhatsAppConfig;
import com.integration_service.entity.IntegrationTemplate;
import com.integration_service.entity.MessageLog;
import com.integration_service.handler.IntegrationHandler;
import com.integration_service.service.MessageLogService;
import com.integration_service.service.WhatsAppClient;
import com.integration_service.util.WhatsAppTemplateBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WhatsAppHandler implements IntegrationHandler {

    private final ObjectMapper objectMapper;
    private final WhatsAppClient client;
    private final MessageLogService logService;

    @Override
    public String getService() {
        return Services.WHATSAPP;
    }

    @Override
    public <T> T parseConfig(IntegrationTemplate template, Class<T> clazz) {
        return null;
    }

    @Override
    public boolean supports(String eventType) {
        return EventTypes.MANUAL_TRIGGER.equals(eventType)
                || EventTypes.PAYMENT_SUCCESS.equals(eventType);
    }

    @Override
    public Object execute(EventRequest event, IntegrationTemplate config) {

        try {
            WhatsAppConfig waConfig = objectMapper.readValue(
                    config.getConfigSchema(),
                    WhatsAppConfig.class
            );

            Map<String, Object> data = event.getData();

            String phone;
            String template;
            Map<String, Object> params;

            if (EventTypes.PAYMENT_SUCCESS.equals(event.getEventType())) {

                phone = (String) data.get("phone");

                template = WhatsAppTemplates.WELCOME;

                params = Map.of(
                        "name", data.get("name"),
                        "amount", String.valueOf(data.get("amount"))
                );

            } else {
                // 🔥 MANUAL FLOW

                phone = (String) data.get("phone");
                template = (String) data.get("template");
                params = (Map<String, Object>) data.get("params");
            }

            Map<String, Object> payload = WhatsAppTemplateBuilder.build(template, params);

            MessageLog log = logService.createPending(phone, template, payload);

            Map<String, Object> response = client.sendMessage(waConfig, phone, payload);

            String messageId = ((Map<String, Object>)
                    ((List<Object>) response.get("messages")).get(0))
                    .get("id").toString();

            logService.markSent(log, messageId, response);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("WhatsApp execution failed", e);
        }
    }
}
