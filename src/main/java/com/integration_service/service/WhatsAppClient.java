package com.integration_service.service;

import com.integration_service.dto.configDto.WhatsAppConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> sendMessage(
            WhatsAppConfig config,
            String phone,
            Map<String, Object> payload
    ) {

        String url = "https://graph.facebook.com/"
                + config.getVersion()
                + "/" + config.getPhoneNumberId()
                + "/messages";

        Map<String, Object> requestBody = new HashMap<>(payload);
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("to", phone);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                entity,
                Map.class
        );

        return response.getBody();
    }
}
