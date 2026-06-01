package com.integration_service.communication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment_transactions",
        indexes = {

                @Index(
                        name = "idx_payment_access_token",
                        columnList = "payment_access_token"
                ),

                @Index(
                        name = "idx_razorpay_order_id",
                        columnList = "razorpay_order_id"
                ),

                @Index(
                        name = "idx_core_transaction_id",
                        columnList = "core_transaction_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID memberId;

    private UUID membershipId;

    /** Core application transaction reference */
    private String coreTransactionId;

    private String correlationId;

    @Column(nullable = false)
    private String integrationType;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "payment_access_token", unique = true)
    private String paymentAccessToken;

    @Column(name = "payment_platform")
    private String paymentPlatform;

    private String razorpayPaymentId;

    @Column(columnDefinition = "TEXT")
    private String universalPaymentLink;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    private WhatsAppDeliveryStatus whatsappStatus;

    @Builder.Default
    private Boolean webhookReceived = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentSyncStatus syncStatus = PaymentSyncStatus.PENDING;

    @Builder.Default
    private Integer retryCount = 0;

    private Integer durationDays;

    private String memberEmail;

    private String memberPhone;

    private String memberName;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime paidAt;

    private LocalDateTime lastSyncAttemptAt;

    private LocalDateTime whatsappSentAt;
}
