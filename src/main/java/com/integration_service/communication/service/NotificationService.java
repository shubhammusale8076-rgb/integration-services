package com.integration_service.communication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    public void sendInvoiceEmail(String toEmail, byte[] pdfBytes) {
        sendInvoiceEmail(toEmail, pdfBytes, null);
    }

    public void sendInvoiceEmail(String toEmail, byte[] pdfBytes, String correlationId) {
        if (toEmail == null || toEmail.isEmpty()) {
            log.warn("Cannot send email: toEmail is null or empty, correlationId={}", correlationId);
            return;
        }

        log.info("Sending invoice email to: {}, correlationId={}", maskEmail(toEmail), correlationId);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Payment Receipt - Elite Gym");
            helper.setText("Hi,\n\nThank you for your payment. Please find your invoice attached.\n\nRegards,\nElite Gym Team");

            helper.addAttachment("invoice.pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Email sent successfully to {}, correlationId={}", maskEmail(toEmail), correlationId);
        } catch (Exception e) {
            log.error("Failed to send email to {}, correlationId={}: {}",
                    maskEmail(toEmail), correlationId, e.getMessage());
            throw new RuntimeException("Failed to send receipt email", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }
}
