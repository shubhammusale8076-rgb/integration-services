package com.integration_service.communication.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAccessInternalResponse {

    private String orderId;

    private Double amount;

    private String currency;

    private String memberName;

    private String email;

    private String phone;

    private String gymName;

    private String razorpayKey;
}
