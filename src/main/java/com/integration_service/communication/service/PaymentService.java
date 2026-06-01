package com.integration_service.communication.service;

import com.integration_service.common.config.CorrelationContext;
import com.integration_service.common.config.TenantContext;
import com.integration_service.communication.dto.*;
import com.integration_service.communication.entity.*;
import com.integration_service.communication.repository.PaymentTransactionRepository;
import com.integration_service.communication.repository.TenantIntegrationRepository;
import com.integration_service.integrations.razorpay.RazorpayCredentialResolver;
import com.integration_service.integrations.razorpay.RazorpayConfig.RazorpayConfig;
import com.integration_service.integrations.razorpay.dto.RazorpayOrderResult;
import com.integration_service.integrations.razorpay.dto.RazorpayPaymentLinkResult;
import com.integration_service.integrations.razorpay.service.RazorpayClientService;
import com.integration_service.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final TenantIntegrationRepository tenantIntegrationRepository;
    private final IntegrationService integrationService;
    private final RazorpayClientService razorpayClientService;
    private final RazorpayCredentialResolver credentialResolver;
    private final PaymentCommunicationService paymentCommunicationService;
    private final InvoiceService invoiceService;
    private final NotificationService notificationService;
    private final ExecutionLogService executionLogService;

    @Transactional
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) throws Exception {

        String correlationId = (request.getCorrelationId() != null
                && !request.getCorrelationId().isBlank())
                ? request.getCorrelationId()
                : CorrelationContext.getOrGenerate();

        CorrelationContext.set(correlationId);

        try {

            log.info(
                    "Creating payment link: tenant={}, member={}, membership={}, transaction={}, correlationId={}",
                    request.getTenantId(),
                    request.getMemberId(),
                    request.getMembershipId(),
                    request.getTransactionId(),
                    correlationId
            );

            // =====================================================
            // 1. Validate Request
            // =====================================================

            if (request.getTenantId() == null) {
                throw new RuntimeException("TenantId is required");
            }

            if (request.getMemberId() == null) {
                throw new RuntimeException("MemberId is required");
            }

            if (request.getTransactionId() == null) {
                throw new RuntimeException("TransactionId is required");
            }

            if (request.getAmount() == null || request.getAmount() <= 0) {
                throw new RuntimeException("Invalid payment amount");
            }

            // =====================================================
            // 2. Prevent Duplicate Payment Links
            // =====================================================

            Optional<PaymentTransaction> existingTransaction = transactionRepository.findByCoreTransactionIdAndStatus(request.getTransactionId(),TransactionStatus.PENDING);

            if (existingTransaction.isPresent()) {

                PaymentTransaction existing = existingTransaction.get();

                log.info(
                        "Existing pending payment link found for transactionId={}",
                        request.getTransactionId()
                );

                return PaymentLinkResponse.builder()
                        .integrationTransactionId(existing.getId())
                        .universalPaymentLink(existing.getUniversalPaymentLink())
                        .razorpayOrderId(existing.getRazorpayOrderId())
                        .transactionId(existing.getCoreTransactionId())
                        .correlationId(existing.getCorrelationId())
                        .whatsappStatus(existing.getWhatsappStatus())
                        .build();
            }

            // =====================================================
            // 3. Resolve Tenant Razorpay Integration
            // =====================================================

            TenantIntegration integration = integrationService
                    .getIntegration(request.getTenantId(), IntegrationType.RAZORPAY)
                    .orElseThrow(() -> new RuntimeException("Razorpay integration not configured for tenant"));

            RazorpayConfig config = credentialResolver.resolve(integration);

            // =====================================================
            // 4. Build Description
            // =====================================================

            String description = "Gym membership payment";

            if (request.getDurationDays() != null) {
                description += " (" + request.getDurationDays() + " days)";
            }

            // =====================================================
            // 5. Build Razorpay Payment Context
            // =====================================================

            RazorpayClientService.PaymentLinkContext linkContext =
                    new RazorpayClientService.PaymentLinkContext(
                            request.getTenantId(),
                            request.getMemberId(),
                            request.getMembershipId(),
                            request.getTransactionId(),
                            correlationId,
                            request.getMemberName(),
                            request.getEmail(),
                            request.getPhone(),
                            description
                    );

            // =====================================================
            // 6. Create Razorpay Payment Link
            // =====================================================

            RazorpayOrderResult orderResult = razorpayClientService.createPaymentLink(config, request.getAmount(), linkContext);

            // =====================================================
            // 7. Save Transaction
            // =====================================================

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .tenantId(request.getTenantId())
                    .memberId(request.getMemberId())
                    .membershipId(request.getMembershipId())
                    .coreTransactionId(request.getTransactionId())
                    .correlationId(correlationId)
                    .integrationType("RAZORPAY")
                    .amount(request.getAmount())
                    .razorpayOrderId(orderResult.getOrderId())
                    .paymentAccessToken(orderResult.getPaymentAccessToken())
                    .universalPaymentLink(orderResult.getUniversalPaymentLink())
                    .paymentPlatform("WHATSAPP")
                    .currency("INR")
                    .status(TransactionStatus.PENDING)
                    .whatsappStatus(WhatsAppDeliveryStatus.PENDING)
                    .webhookReceived(false)
                    .syncStatus(PaymentSyncStatus.PENDING)
                    .retryCount(0)
                    .durationDays(request.getDurationDays())
                    .memberEmail(request.getEmail())
                    .memberPhone(request.getPhone())
                    .memberName(request.getMemberName())
                    .build();

            transaction = transactionRepository.save(transaction);

            // =====================================================
            // 8. Log Success
            // =====================================================

            executionLogService.logSuccess(
                    IntegrationType.RAZORPAY,
                    "PAYMENT_LINK_CREATE",
                    request,
                    orderResult
            );

            // =====================================================
            // 9. Send WhatsApp
            // =====================================================

            WhatsAppDeliveryStatus whatsappStatus;
            String whatsappError = null;

            try {

                whatsappStatus = paymentCommunicationService.sendPaymentLink(transaction.getId());

                if (whatsappStatus == WhatsAppDeliveryStatus.SENT) {

                    log.info(
                            "Payment link WhatsApp sent successfully: transactionId={}",
                            transaction.getId()
                    );

                } else {

                    log.warn(
                            "Payment link WhatsApp not delivered: transactionId={}, status={}",
                            transaction.getId(),
                            whatsappStatus
                    );
                }

            } catch (Exception ex) {

                whatsappStatus = WhatsAppDeliveryStatus.FAILED;
                whatsappError = ex.getMessage();

                log.warn(
                        "WhatsApp send failed but payment link created successfully: transactionId={}, error={}",
                        transaction.getId(),
                        ex.getMessage()
                );

                executionLogService.logFailure(IntegrationType.WHATSAPP, "SEND_PAYMENT_LINK", request, ex);
            }

            // =====================================================
            // 10. Update WhatsApp Status
            // =====================================================

            transaction.setWhatsappStatus(whatsappStatus);

            if (whatsappError != null) {
                transaction.setLastError(whatsappError);
            }

            transactionRepository.save(transaction);

            // =====================================================
            // 11. Return Response
            // =====================================================

            return PaymentLinkResponse.builder()
                    .integrationTransactionId(transaction.getId())
                    .universalPaymentLink(orderResult.getUniversalPaymentLink())
                    .razorpayOrderId(orderResult.getOrderId())
                    .transactionId(request.getTransactionId())
                    .correlationId(correlationId)
                    .whatsappStatus(whatsappStatus)
                    .whatsappError(whatsappError)
                    .build();

        } catch (Exception ex) {

            log.error(
                    "Payment link generation failed: tenant={}, member={}, error={}",
                    request.getTenantId(),
                    request.getMemberId(),
                    ex.getMessage(),
                    ex
            );

            executionLogService.logFailure(
                    IntegrationType.RAZORPAY,
                    "PAYMENT_LINK_CREATE",
                    request,
                    ex
            );

            throw ex;

        } finally {

            CorrelationContext.clear();
        }
    }

    @Transactional
    public PaymentLinkResponse resendPaymentLinkWhatsApp(UUID transactionId) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found"));

        if (transaction.getCorrelationId() != null) {
            CorrelationContext.set(transaction.getCorrelationId());
        }
        TenantContext.setTenant(transaction.getTenantId().toString());

        WhatsAppDeliveryStatus status = paymentCommunicationService.sendPaymentLink(transactionId);

        return PaymentLinkResponse.builder()
                .integrationTransactionId(transaction.getId())
                .universalPaymentLink(transaction.getUniversalPaymentLink())
                .razorpayOrderId(transaction.getRazorpayOrderId())
                .transactionId(transaction.getCoreTransactionId())
                .correlationId(transaction.getCorrelationId())
                .whatsappStatus(status)
                .build();
    }

    @Transactional
    public void sendPaymentReceiptEmail(PaymentReceiptEmailRequest request) {
        String correlationId = request.getCorrelationId() != null
                ? request.getCorrelationId()
                : CorrelationContext.getOrGenerate();

        PaymentTransaction transaction = transactionRepository.findById(request.getPaymentTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found"));

        String email = request.getEmail() != null ? request.getEmail() : transaction.getMemberEmail();
        byte[] pdf = invoiceService.generateInvoicePdf(transaction);
        notificationService.sendInvoiceEmail(email, pdf, correlationId);

        executionLogService.logSuccess(
                IntegrationType.RAZORPAY,
                "PAYMENT_RECEIPT_EMAIL",
                request,
                "SENT"
        );
    }

    public PaymentAccessInternalResponse getPaymentAccess(String token) {

        PaymentTransaction transaction = transactionRepository.findByPaymentAccessToken(token)
                        .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {

            throw new RuntimeException(
                    "Payment already processed"
            );
        }
        TenantIntegration integration = tenantIntegrationRepository.findByTenantIdAndIntegrationType(transaction.getTenantId(), IntegrationType.RAZORPAY)
                        .orElseThrow(() -> new RuntimeException("Razorpay integration not found"));

        RazorpayConfig config = credentialResolver.resolve(integration);

        return PaymentAccessInternalResponse.builder()
                .orderId(transaction.getRazorpayOrderId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .memberName(transaction.getMemberName())
                .email(transaction.getMemberEmail())
                .phone(transaction.getMemberPhone())
                .gymName("Elite Gym")
                .razorpayKey(config.getKey())
                .build();

    }
}
