package com.integration_service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class EventRequest {

    private String eventType;
    private Map<String, Object> data;
}
