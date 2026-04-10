package com.integration_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GymCallbackService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void notifyPaymentSuccess(Map<String, Object> data) {

        String gymApiUrl = "http://gym-app/api/payments/webhook";

        restTemplate.postForObject(gymApiUrl, data, String.class);
    }
}
