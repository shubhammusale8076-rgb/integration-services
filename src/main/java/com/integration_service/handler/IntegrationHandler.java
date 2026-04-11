package com.integration_service.handler;

import com.integration_service.dto.EventRequest;
import com.integration_service.entity.IntegrationTemplate;

public interface IntegrationHandler {

    boolean supports(String eventType);

    Object  execute(EventRequest event, IntegrationTemplate config);
    String getService(); // RAZORPAY

    <T> T parseConfig(IntegrationTemplate template, Class<T> clazz);
}