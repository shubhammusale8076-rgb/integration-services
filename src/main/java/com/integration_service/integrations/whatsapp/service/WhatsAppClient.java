package com.integration_service.integrations.whatsapp.service;

import com.integration_service.communication.dto.IntegrationHealthResult;
import com.integration_service.dto.WhatsAppResponse;
import com.integration_service.dto.configDto.WhatsAppConfig;
import com.integration_service.handler.HealthCheckSupport;
import com.integration_service.handler.IntegrationHandlerHealthContext;

import com.integration_service.integrations.whatsapp.dto.CreateMetaTemplateRequestDto;
import com.integration_service.integrations.whatsapp.dto.MetaTemplateCreateResponse;
import com.integration_service.integrations.whatsapp.dto.MetaTemplateResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Service;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppClient {

    private final WebClient webClient;

    @Value("${whatsapp.api-version:v22.0}")
    private String whatsappApiVersion;

    public WhatsAppResponse sendMessage(WhatsAppConfig config, String phone, Map<String, Object> payload) {

        String url =
                "https://graph.facebook.com/"
                        + whatsappApiVersion
                        + "/"
                        + config.getPhoneNumberId()
                        + "/messages";

        Map<String, Object> requestBody = new HashMap<>(payload);

        requestBody.put("messaging_product", "whatsapp");

        requestBody.put("to", phone);

        return webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer "
                                + config.getAccessToken()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(WhatsAppResponse.class)
                .block();
    }

    public void validateToken(String token) {
        checkTokenHealth(token);
    }

    public IntegrationHealthResult checkTokenHealth(String token) {
        try {
            webClient.get()
                    .uri("https://graph.facebook.com/v22.0/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            IntegrationHandlerHealthContext.setLastError(null);
            return HealthCheckSupport.healthy();
        } catch (WebClientResponseException ex) {
            log.warn("WhatsApp token health check failed: status={}, body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            return HealthCheckSupport.fromThrowable(ex);
        } catch (Exception ex) {
            log.warn("WhatsApp token health check failed: {}", ex.getMessage());
            return HealthCheckSupport.fromThrowable(ex);
        }
    }

    public void sendHelloWorldTemplate(String token, String phoneNumberId, String recipient) {

        String url = "https://graph.facebook.com/v22.0/" + phoneNumberId + "/messages";

        Map<String, Object> payload =
                Map.of(
                        "messaging_product",
                        "whatsapp",

                        "to",
                        recipient,

                        "type",
                        "template",

                        "template",
                        Map.of(
                                "name",
                                "hello_world",

                                "language",
                                Map.of(
                                        "code",
                                        "en_US"
                                )
                        )
                );

        webClient.post()
                .uri(url)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public MetaTemplateResponseDto fetchTemplates(WhatsAppConfig config) {

        try {

            String url = "https://graph.facebook.com/v22.0/" + config.getBusinessAccountId() + "/message_templates";

            return webClient
                    .get()
                    .uri(url)
                    .headers(headers -> {headers.setBearerAuth(config.getAccessToken());
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,

                            response ->
                                    response.bodyToMono(String.class)
                                            .flatMap(error -> {
                                                log.error("Meta API 4XX error: {}", error);
                                                return Mono.error(new RuntimeException("Meta API client error."));
                                            })
                    )

                    .onStatus(
                            HttpStatusCode::is5xxServerError,

                            response ->
                                    response.bodyToMono(String.class)
                                            .flatMap(error -> {
                                                log.error("Meta API 5XX error: {}", error);

                                                return Mono.error(new RuntimeException("Meta API server error."));
                                            })
                    )

                    .bodyToMono(MetaTemplateResponseDto.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

        } catch (Exception ex) {

            log.error("Failed to fetch WhatsApp templates: {}", ex.getMessage(), ex);

            throw new RuntimeException("Meta template fetch failed.");
        }
    }

    public MetaTemplateCreateResponse createTemplate(WhatsAppConfig config, CreateMetaTemplateRequestDto request) {

        String url =
                "https://graph.facebook.com/v22.0/"
                        + config.getBusinessAccountId()
                        + "/message_templates";

        Map<String, Object> payload = Map.of(

                "name", request.getName(),

                "category", request.getCategory(),

                "language", request.getLanguage(),

                "components", List.of(

                        Map.of(
                                "type", "BODY",

                                "text",
                                request.getBody()
                        )
                )
        );

        return webClient
                .post()
                .uri(url)
                .headers(headers -> {
                    headers.setBearerAuth(config.getAccessToken());
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(MetaTemplateCreateResponse.class)
                .block();
    }
}