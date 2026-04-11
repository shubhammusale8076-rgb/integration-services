package com.integration_service.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_templates", indexes = {
        @Index(name = "idx_template_tenant_id", columnList = "tenantId")
})
@Getter
@Setter
public class IntegrationTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    private String tenantId;

    private String service; // RAZORPAY, WHATSAPP, GOOGLE

    private String authType; // OAUTH, API_KEY

    @Column(columnDefinition = "TEXT")
    private String configSchema; // JSON schema for form

    private boolean enabled;

    private String mode; // MANUAL / AUTOMATED

    private Boolean active;

    private LocalDateTime createdAt;
}
