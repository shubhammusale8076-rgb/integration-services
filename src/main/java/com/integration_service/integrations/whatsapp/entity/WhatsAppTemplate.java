package com.integration_service.integrations.whatsapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_whatsapp_templates", indexes = {
        @Index(name = "idx_wa_template_tenant_code", columnList = "tenantId, templateCode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID globalTemplateId;

    @Column(nullable = false, unique = true)
    private String metaTemplateId;

    @Column(nullable = false)
    private String templateName; // WELCOME, REMINDER, OFFER

    @Column(nullable = false)
    private String languageCode;

    private String eventKey;

    @Column(nullable = false)
    private String status;

    @Builder.Default
    private Boolean active = false;

    private String rejectionReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
