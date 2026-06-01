package com.integration_service.integrations.razorpay.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RazorpayOrderResult {

    private String orderId;

    private String paymentAccessToken;

    private String universalPaymentLink;
}