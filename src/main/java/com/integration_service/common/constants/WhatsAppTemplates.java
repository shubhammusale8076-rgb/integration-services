package com.integration_service.common.constants;

public class WhatsAppTemplates {

    public static final String WELCOME = "WELCOME";
    public static final String REMINDER = "REMINDER";
    public static final String EXPIRY = "EXPIRY";
    public static final String LEAD_FOLLOWUP = "LEAD_FOLLOWUP";

    public static String getTemplateName(String template) {
        return switch (template) {
            case WELCOME -> "welcome_message";
            case REMINDER -> "reminder_message";
            case EXPIRY -> "expiry_message";
            case LEAD_FOLLOWUP -> "lead_followup_message";
            default -> throw new RuntimeException("Invalid template");
        };
    }
}
