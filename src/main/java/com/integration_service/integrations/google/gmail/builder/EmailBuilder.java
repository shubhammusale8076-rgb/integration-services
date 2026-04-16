package com.integration_service.integrations.google.gmail.builder;

import java.util.Base64;

public class EmailBuilder {

    public static String build(String to, String subject, String body) {

        String message = "From: me\r\n" +
                "To: " + to + "\r\n" +
                "Subject: " + subject + "\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n\r\n" +
                body;

        return Base64.getUrlEncoder().encodeToString(message.getBytes());
    }

    public static String buildHtml(String to, String subject, String html) {

        String message = "From: me\r\n" +
                "To: " + to + "\r\n" +
                "Subject: " + subject + "\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n\r\n" +
                html;

        return Base64.getUrlEncoder().encodeToString(message.getBytes());
    }
}
