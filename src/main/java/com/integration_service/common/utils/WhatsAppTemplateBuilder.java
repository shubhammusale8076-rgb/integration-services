package com.integration_service.common.utils;

import com.integration_service.common.constants.WhatsAppTemplates;

import java.util.List;
import java.util.Map;

public class WhatsAppTemplateBuilder {

    public static Map<String, Object> build(String template, Map<String, Object> params) {

        List<Map<String, Object>> parameters = params.values().stream()
                .map(val -> Map.<String, Object>of(
                        "type", "text",
                        "text", val.toString()
                ))
                .toList();

        return Map.of(
                "type", "template",
                "template", Map.of(
                        "name", WhatsAppTemplates.getTemplateName(template),
                        "language", Map.of("code", "en"),
                        "components", List.of(
                                Map.of(
                                        "type", "body",
                                        "parameters", parameters
                                )
                        )
                )
        );
    }
}
