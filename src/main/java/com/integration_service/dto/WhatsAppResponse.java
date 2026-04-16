package com.integration_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class WhatsAppResponse {
    private List<WhatsAppMessage> messages;

    @Data
    public static class WhatsAppMessage {
        private String id;
    }
}
