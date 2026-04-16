package com.integration_service.integrations.google.gmail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GmailClient {

    private final RestTemplate restTemplate ;

    public void sendEmail(String accessToken, String rawMessage) {

        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

        Map<String, String> body = Map.of("raw", rawMessage);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, request, String.class);
    }
}
