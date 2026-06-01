package com.integration_service.integrations.razorpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayPaymentLinkResult {
    private String paymentLinkId;
    private String shortUrl;
}
