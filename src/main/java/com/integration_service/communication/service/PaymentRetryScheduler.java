package com.integration_service.communication.service;

import com.integration_service.communication.entity.PaymentSyncStatus;
import com.integration_service.communication.entity.PaymentTransaction;
import com.integration_service.communication.entity.TransactionStatus;
import com.integration_service.communication.entity.WhatsAppDeliveryStatus;
import com.integration_service.communication.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRetryScheduler {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentCoreSyncService paymentCoreSyncService;
    private final PaymentCommunicationService paymentCommunicationService;

    @Value("${integration.payment.retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Scheduled(fixedDelayString = "${integration.payment.retry.fixed-delay-ms:300000}")
    public void retryPendingOperations() {
        log.debug("Payment retry scheduler started");
        retryPendingCoreSync();
        retryFailedWhatsApp();
    }

    private void retryPendingCoreSync() {
        List<PaymentTransaction> pending = paymentTransactionRepository
                .findBySyncStatusAndRetryCountLessThan(PaymentSyncStatus.PENDING, maxRetryAttempts);

        for (PaymentTransaction transaction : pending) {
            if (transaction.getStatus() != TransactionStatus.PAID) {
                continue;
            }
            log.info("Retrying core payment sync: transactionId={}, retryCount={}",
                    transaction.getId(), transaction.getRetryCount());
            paymentCoreSyncService.syncPaymentToCore(transaction);
        }
    }

    private void retryFailedWhatsApp() {
        List<PaymentTransaction> failed = paymentTransactionRepository
                .findByWhatsappStatusAndRetryCountLessThan(WhatsAppDeliveryStatus.FAILED, maxRetryAttempts);

        for (PaymentTransaction transaction : failed) {
            if (transaction.getRazorpayOrderId() == null) {
                continue;
            }
            log.info("Retrying WhatsApp payment link send: transactionId={}, retryCount={}",
                    transaction.getId(), transaction.getRetryCount());
            paymentCommunicationService.sendPaymentLink(transaction.getId());
        }
    }
}
