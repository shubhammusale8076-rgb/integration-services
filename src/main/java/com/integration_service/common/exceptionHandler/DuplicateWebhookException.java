package com.integration_service.common.exceptionHandler;

public class DuplicateWebhookException extends RuntimeException {

    public DuplicateWebhookException(String message) {
        super(message);
    }
}