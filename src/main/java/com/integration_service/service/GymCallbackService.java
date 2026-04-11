package com.integration_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GymCallbackService {

    private final RestTemplate restTemplate;

    @Value("${gym.app.url}")
    private String gymApiUrl;

    public void notifyPaymentSuccess(Map<String, Object> data) {

        restTemplate.postForObject(gymApiUrl, data, String.class);
    }
}
