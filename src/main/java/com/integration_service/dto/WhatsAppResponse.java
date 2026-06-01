package com.integration_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppResponse {

    private String messaging_product;

    private List<Contact> contacts;

    private List<WhatsAppMessage> messages;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {

        private String input;

        private String wa_id;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsAppMessage {

        private String id;
    }
}
