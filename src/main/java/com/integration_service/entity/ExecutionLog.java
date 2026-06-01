package com.integration_service.entity;

import com.integration_service.communication.entity.IntegrationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "execution_logs", indexes = {
        @Index(name = "idx_execution_tenant_id", columnList = "tenantId")
})
public class ExecutionLog {

    @Id
    @GeneratedValue
    private UUID id;

    private String tenantId;

    private IntegrationType service;     // RAZORPAY
    private String eventType;   // PAYMENT_CREATE

    private String status;      // SUCCESS / FAILED

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(columnDefinition = "TEXT")
    private String error;

    private LocalDateTime createdAt;
}
