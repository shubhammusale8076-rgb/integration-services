package com.integration_service.communication.repository;

import com.integration_service.communication.entity.PaymentSyncStatus;
import com.integration_service.communication.entity.PaymentTransaction;
import com.integration_service.communication.entity.TransactionStatus;
import com.integration_service.communication.entity.WhatsAppDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);

    List<PaymentTransaction> findBySyncStatusAndRetryCountLessThan(PaymentSyncStatus syncStatus, int maxRetryCount);

    List<PaymentTransaction> findByWhatsappStatusAndRetryCountLessThan(WhatsAppDeliveryStatus whatsappStatus, int maxRetryCount);

    Optional<PaymentTransaction> findByCoreTransactionIdAndStatus(String transactionId, TransactionStatus transactionStatus);

    Optional<PaymentTransaction> findByPaymentAccessToken(String token);
}
