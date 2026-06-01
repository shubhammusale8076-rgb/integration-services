package com.integration_service.integrations.razorpay.service;

import com.integration_service.integrations.razorpay.RazorpayConfig.RazorpayConfig;
import com.integration_service.integrations.razorpay.dto.RazorpayOrderResult;
import com.integration_service.integrations.razorpay.dto.RazorpayPaymentLinkResult;
import com.razorpay.Order;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RazorpayClientService {

    @Value("${integration.payment.link-expiry-seconds:3600}")
    private long linkExpirySeconds;

    @Value("${frontend.app.url}")
    private String appBaseUrl;

    public RazorpayOrderResult createPaymentLink(RazorpayConfig config, double amountInRupees, PaymentLinkContext context) throws RazorpayException {

        log.info("Creating Razorpay payment link: tenant={}, member={}, correlationId={}",
                context.tenantId(), context.memberId(), context.correlationId());

        RazorpayClient client = new RazorpayClient(config.getKey(), config.getKeySecret());

        JSONObject request = new JSONObject();
        request.put("amount", (int) Math.round(amountInRupees * 100));
        request.put("currency", "INR");
        request.put("receipt", context.coreTransactionId());

        JSONObject notes = new JSONObject();
        notes.put("tenantId", context.tenantId().toString());
        notes.put("memberId", context.memberId().toString());

        if (context.membershipId() != null) {
            notes.put("membershipId", context.membershipId().toString());
        }

        if (context.correlationId() != null) {

            notes.put("correlationId", context.correlationId());
        }

        request.put("notes", notes);

        Order order = client.orders.create(request);

        String orderId = order.get("id");

        String paymentAccessToken = UUID.randomUUID().toString();

        log.info("Razorpay order created: orderId={}, correlationId={}", orderId, context.correlationId());

        String universalPaymentLink = appBaseUrl + "/payment/" + paymentAccessToken;


        log.info(
                "Razorpay order created: orderId={}, correlationId={}",
                orderId,
                context.correlationId()
        );

        return RazorpayOrderResult.builder()
                .orderId(orderId)
                .paymentAccessToken(paymentAccessToken)
                .universalPaymentLink(universalPaymentLink)
                .build();
    }

    public void validateCredentials(RazorpayConfig config) throws RazorpayException {
        RazorpayClient client = new RazorpayClient(config.getKey(), config.getKeySecret());
        client.orders.fetchAll(null);
    }

    public record PaymentLinkContext(
            UUID tenantId,
            UUID memberId,
            UUID membershipId,
            String coreTransactionId,
            String correlationId,
            String memberName,
            String email,
            String phone,
            String description
    ) {
    }
}
