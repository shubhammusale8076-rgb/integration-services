package com.integration_service.integration.parser;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ParserFactory {

    private final Map<String, WebhookParser> parsers;

    public ParserFactory(List<WebhookParser> parserList) {
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(WebhookParser::getSource, Function.identity()));
    }

    public WebhookParser getParser(String source) {
        return Optional.ofNullable(parsers.get(source.toUpperCase()))
                .orElseThrow(() -> new RuntimeException("No parser found for source: " + source));
    }
}

