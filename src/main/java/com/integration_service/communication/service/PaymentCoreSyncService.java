package com.integration_service.communication.service;

import com.integration_service.common.config.CorrelationContext;
import com.integration_service.communication.client.CoreClient;
import com.integration_service.communication.dto.CorePaymentConfirmRequest;
import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.communication.entity.PaymentSyncStatus;
import com.integration_service.communication.entity.PaymentTransaction;
import com.integration_service.communication.entity.TransactionStatus;
import com.integration_service.communication.repository.PaymentTransactionRepository;
import com.integration_service.service.ExecutionLogService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCoreSyncService {

    private final CoreClient coreClient;
    private final PaymentTransactionRepository transactionRepository;
    private final ExecutionLogService executionLogService;

    @Transactional
    public boolean syncPaymentToCore(PaymentTransaction transaction) {
        if (transaction.getSyncStatus() == PaymentSyncStatus.SYNCED) {
            log.info("Payment already synced to core: transactionId={}", transaction.getId());
            return true;
        }

        if (transaction.getCorrelationId() != null) {
            CorrelationContext.set(transaction.getCorrelationId());
        }

        transaction.setLastSyncAttemptAt(LocalDateTime.now());

        CorePaymentConfirmRequest request = CorePaymentConfirmRequest.builder()
                .tenantId(transaction.getTenantId())
                .memberId(transaction.getMemberId())
                .membershipId(transaction.getMembershipId())
                .transactionId(transaction.getCoreTransactionId())
                .correlationId(transaction.getCorrelationId())
                .paymentId(transaction.getRazorpayPaymentId())
                .orderId(transaction.getRazorpayOrderId())
                .amount(transaction.getAmount())
                .paymentStatus(transaction.getStatus())
                .currency(transaction.getCurrency())
                .build();

        try {
            log.info("Syncing payment to core: transactionId={}, correlationId={}, paymentId={}",
                    transaction.getId(), transaction.getCorrelationId(), transaction.getRazorpayPaymentId());

            coreClient.confirmPayment(request);

            transaction.setSyncStatus(PaymentSyncStatus.SYNCED);
            transaction.setLastError(null);
            transactionRepository.save(transaction);

            executionLogService.logSuccess(
                    IntegrationType.RAZORPAY,
                    "CORE_PAYMENT_CONFIRM",
                    request,
                    "SYNCED"
            );

            log.info("Core payment confirm succeeded: transactionId={}", transaction.getId());
            return true;

        } catch (FeignException ex) {
            log.error("Core payment confirm failed: transactionId={}, status={}, message={}",
                    transaction.getId(), ex.status(), ex.getMessage());

            transaction.setSyncStatus(PaymentSyncStatus.PENDING);
            transaction.setLastError("Core sync failed: " + ex.getMessage());
            transaction.setRetryCount(transaction.getRetryCount() != null ? transaction.getRetryCount() + 1 : 1);
            transactionRepository.save(transaction);

            executionLogService.logFailure(
                    IntegrationType.RAZORPAY,
                    "CORE_PAYMENT_CONFIRM",
                    request,
                    ex
            );
            return false;

        } catch (Exception ex) {
            log.error("Core payment confirm error: transactionId={}, message={}",
                    transaction.getId(), ex.getMessage());

            transaction.setSyncStatus(PaymentSyncStatus.PENDING);
            transaction.setLastError(ex.getMessage());
            transaction.setRetryCount(transaction.getRetryCount() != null ? transaction.getRetryCount() + 1 : 1);
            transactionRepository.save(transaction);

            executionLogService.logFailure(
                    IntegrationType.RAZORPAY,
                    "CORE_PAYMENT_CONFIRM",
                    request,
                    ex
            );
            return false;
        } finally {
            CorrelationContext.clear();
        }
    }

    @Transactional
    public void markPaymentPaid(PaymentTransaction transaction, String razorpayPaymentId, int amountPaise) {
        if (transaction.getStatus() == TransactionStatus.PAID
                && transaction.getRazorpayPaymentId() != null
                && transaction.getRazorpayPaymentId().equals(razorpayPaymentId)) {
            log.info("Payment already marked paid: orderId={}", transaction.getRazorpayOrderId());
            return;
        }

        transaction.setStatus(TransactionStatus.PAID);
        transaction.setRazorpayPaymentId(razorpayPaymentId);
        transaction.setPaidAt(LocalDateTime.now());
        transaction.setWebhookReceived(true);
        if (amountPaise > 0) {
            transaction.setAmount(amountPaise / 100.0);
        }
        transactionRepository.save(transaction);
    }
}
