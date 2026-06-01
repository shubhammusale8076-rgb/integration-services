package com.integration_service.communication.service;

import com.integration_service.communication.entity.PaymentTransaction;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final SpringTemplateEngine templateEngine;

    public byte[] generateInvoicePdf(PaymentTransaction transaction) {
        log.info("Generating invoice PDF for transaction: {}", transaction.getId());

        try {
            Context context = new Context();
            context.setVariable("gymName", "Elite Gym");
            context.setVariable("memberName", "Member " + transaction.getMemberId().toString().substring(0, 8));
            context.setVariable("amount", transaction.getAmount());
            context.setVariable("duration", transaction.getDurationDays() + " Days");
            context.setVariable("paymentDate", transaction.getPaidAt());
            context.setVariable("invoiceId", transaction.getRazorpayPaymentId());

            String html = templateEngine.process("invoice", context);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "");
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate invoice PDF", e);
            throw new RuntimeException("Invoice generation failed", e);
        }
    }
}
