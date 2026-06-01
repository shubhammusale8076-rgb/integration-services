package com.integration_service.integrations.google.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.integration_service.communication.entity.IntegrationStatus;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.TenantIntegration;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.dto.ResponseDto;
import com.integration_service.integrations.google.dto.GoogleConfig;
import com.integration_service.integrations.google.dto.GooglePasswordResetRequestDto;
import com.integration_service.service.ExecutionLogService;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GmailService {

    private final TenantIntegrationRepository integrationRepository;
    private final ExecutionLogService executionLogService;
    private final SpringTemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;


    public ResponseDto sendTemporaryPasswordMessage(GooglePasswordResetRequestDto request) {

        try{
            TenantIntegration integration = integrationRepository.findByTenantIdAndIntegrationType(request.getTenantId(), IntegrationType.GOOGLE)
                    .orElseThrow(() -> new RuntimeException("Google integration not connected"));

            if (integration.getStatus() != IntegrationStatus.CONNECTED) {
                log.warn("Google not connected for tenant {}, skipping link send", request.getTenantId());

                return ResponseDto.builder()
                        .code(400)
                        .message("Google Integration is not Connected")
                        .build();
            }

            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setAccessToken(new AccessToken(integration.getAccessToken(), null))
                    .setRefreshToken(integration.getRefreshToken())
                    .build();

            // =====================================
            // AUTO REFRESH TOKEN
            // =====================================

            credentials.refreshIfExpired();

            if (credentials.getAccessToken() != null) {
                integration.setAccessToken(credentials.getAccessToken().getTokenValue());

                integration.setUpdatedAt(LocalDateTime.now());
                integrationRepository.save(integration);
                log.info("Google access token refreshed successfully");
            }

            Gmail gmail = buildGmailClient(credentials);

            String html = buildPasswordResetHtml(request);

            GoogleConfig googleConfig = getGoogleConfig(integration);

            MimeMessage mimeMessage = createEmail(
                    request.getEmail(),
                    googleConfig.getEmail(),
                    "Your GymFlow Password Reset",
                    html
            );

            com.google.api.services.gmail.model.Message gmailMessage = buildMessage(mimeMessage);

            com.google.api.services.gmail.model.Message response = gmail.users()
                            .messages()
                            .send("me", gmailMessage)
                            .execute();


            executionLogService.logSuccess(
                    IntegrationType.GMAIL,
                    "Password Send",
                    Map.of( "member name", request.getMemberName()),
                    response
            );
            return ResponseDto.builder()
                    .code(200)
                    .message("Temporary password sent successfully")
                    .build();

        } catch (Exception ex) {
            log.error("Failed to send mail: {}",  ex.getMessage());
            executionLogService.logFailure(
                    IntegrationType.GMAIL,
                    "Password Send",
                    Map.of( "member name", request.getMemberName()),
                    ex

            );

            return ResponseDto.builder()
                    .code(400)
                    .message("Failed to send Mail")
                    .build();
        }
    }

    private GoogleConfig getGoogleConfig(TenantIntegration integration) {

        try {

            return objectMapper.readValue(integration.getMetadata(), GoogleConfig.class);

        } catch (Exception ex) {

            throw new RuntimeException(
                    "Failed to parse google metadata",
                    ex
            );
        }
    }

    private Gmail buildGmailClient(UserCredentials credentials) throws Exception {

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        )
                .setApplicationName("GymFlow")
                .build();
    }

    private String buildPasswordResetHtml(GooglePasswordResetRequestDto request) {

        Context context = new Context();

        context.setVariable(
                "memberName",
                request.getMemberName()
        );

        context.setVariable(
                "temporaryPassword",
                request.getTemporaryPassword()
        );

        return templateEngine.process(
                "email/password-reset",
                context
        );
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyHtml) throws Exception {

        Session session = Session.getDefaultInstance(new Properties(), null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));

        email.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

        email.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();

        mimeBodyPart.setContent(bodyHtml, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();

        multipart.addBodyPart(mimeBodyPart);

        email.setContent(multipart);

        return email;
    }

    private com.google.api.services.gmail.model.Message
    buildMessage(MimeMessage mimeMessage) throws Exception {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        mimeMessage.writeTo(buffer);

        byte[] bytes = buffer.toByteArray();

        String encodedEmail = Base64.getUrlEncoder()
                .encodeToString(bytes);

        com.google.api.services.gmail.model.Message message = new com.google.api.services.gmail.model.Message();

        message.setRaw(encodedEmail);

        return message;
    }
}
