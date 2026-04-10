package com.integration_service.razorpay.service;

import com.integration_service.razorpay.RazorpayConfig.RazorpayConfig;
import org.springframework.stereotype.Service;

@Service
public class RazorpayClientService {

    public String createPaymentLink(RazorpayConfig config,
                                    int amount,
                                    String phone) {

        // TODO: use Razorpay SDK or HTTP call

        // dummy response for now
        return "https://rzp.link/payment123";
    }
}