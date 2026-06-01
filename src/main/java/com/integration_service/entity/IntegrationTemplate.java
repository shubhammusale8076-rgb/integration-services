package com.integration_service.entity;


import com.integration_service.communication.entity.IntegrationType;
import com.integration_service.enums.IntegrationAuthType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_templates", indexes = {
        @Index(name = "idx_template_service", columnList = "service")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private IntegrationType service; // RAZORPAY, WHATSAPP, GOOGLE, STRIPE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationAuthType authType; // OAUTH, API_KEY, WEBHOOK_ONLY

    @Column(nullable = false)
    private String displayName;

    private String description;

    private String icon;

    private String iconColor;

    private String iconBg;

    private boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String configSchema; // JSON schema for form

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
