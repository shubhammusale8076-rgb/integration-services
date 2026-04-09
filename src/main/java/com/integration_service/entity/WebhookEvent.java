package com.integration_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WebhookEvent {

    @Id
    @GeneratedValue
    private UUID id;

    private String tenant_id;

    private String source; // RAZORPAY, WHATSAPP, STRIPE

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String externalEventId; // from provider (IMPORTANT for idempotency)

    @Enumerated(EnumType.STRING)
    private EventStatus status; // PENDING, PROCESSING, DONE, FAILED

    private Integer retryCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
