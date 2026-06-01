package com.integration_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class TestWebhookController {

    @GetMapping("/webhooks/test")
    public String testWebhook() {
        return "Webhook Working";
    }
}
