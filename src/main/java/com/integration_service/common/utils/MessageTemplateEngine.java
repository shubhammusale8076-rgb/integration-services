package com.integration_service.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MessageTemplateEngine {

    private static final Pattern PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");

    public String replaceVariables(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }

        StringBuilder result = new StringBuilder();
        Matcher matcher = PATTERN.matcher(template);
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(template, lastEnd, matcher.start());
            String key = matcher.group(1).trim();
            String value = variables.get(key);

            if (value != null) {
                result.append(value);
            } else {
                log.warn("Variable '{}' not found in provided map for template replacement", key);
                result.append(matcher.group(0)); // Keep the placeholder if value is missing
            }
            lastEnd = matcher.end();
        }
        result.append(template.substring(lastEnd));

        return result.toString();
    }
}
