package com.integration_service.communication.client;

import com.integration_service.communication.dto.CoreNotificationRequest;
import com.integration_service.communication.dto.CorePaymentConfirmRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "core-client", url = "${core.app.url}", configuration = CoreClientConfig.class)
public interface CoreClient {

    @PostMapping("/internal/payments/success")
    void notifyPaymentSuccess(@RequestBody CoreNotificationRequest request);

    @PostMapping("/internal/payments/confirm")
    void confirmPayment(@RequestBody CorePaymentConfirmRequest request);
}
