package com.integration_service.integrations.google.gmail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final SpringTemplateEngine templateEngine;

    public String process(String templateName, Map<String, Object> data) {

        Context context = new Context();
        context.setVariables(data);

        return templateEngine.process(templateName, context);
    }
}
