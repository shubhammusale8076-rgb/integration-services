package com.integration_service.communication.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.integration_service.common.utils.AesEncryptor;
import com.integration_service.communication.dto.IntegrationHealthResult;
import com.integration_service.entity.IntegrationTemplate;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_integrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenantId", "integrationType"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationType integrationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    @JsonIgnore
    private IntegrationTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationStatus status;

    private boolean enabled;

    private String mode; // MANUAL / AUTOMATED

    @Convert(converter = AesEncryptor.class)
    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Convert(converter = AesEncryptor.class)
    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    private LocalDateTime expiryTime;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    private IntegrationHealthStatus healthStatus;

    private LocalDateTime lastValidatedAt;

    private LocalDateTime lastHealthCheckAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private Boolean reauthRequired;

    private Integer consecutiveFailures;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void applyHealthDefaults() {
        if (healthStatus == null) {
            healthStatus = mapStatusToHealth(status);
        }
        if (consecutiveFailures == null) {
            consecutiveFailures = 0;
        }
        if (reauthRequired == null) {
            reauthRequired = false;
        }
    }

    public static IntegrationHealthStatus mapStatusToHealth(IntegrationStatus integrationStatus) {
        if (integrationStatus == null) {
            return IntegrationHealthStatus.FAILED;
        }
        return switch (integrationStatus) {
            case CONNECTED -> IntegrationHealthStatus.CONNECTED;
            case DISCONNECTED -> IntegrationHealthStatus.DISCONNECTED;
            case FAILED -> IntegrationHealthStatus.FAILED;
        };
    }

    public void applySuccessfulHealthCheck(LocalDateTime checkedAt) {
        this.healthStatus = IntegrationHealthStatus.CONNECTED;
        this.lastValidatedAt = checkedAt;
        this.lastHealthCheckAt = checkedAt;
        this.lastError = null;
        this.reauthRequired = false;
        this.consecutiveFailures = 0;
    }

    public void applyFailedHealthCheck(IntegrationHealthResult result, int degradedThreshold) {
        LocalDateTime checkedAt = result.getCheckedAt() != null ? result.getCheckedAt() : LocalDateTime.now();
        this.lastHealthCheckAt = checkedAt;
        this.lastError = result.getError();
        this.reauthRequired = result.isReauthRequired();
        this.healthStatus = result.getStatus() != null ? result.getStatus() : IntegrationHealthStatus.FAILED;

        int failures = (this.consecutiveFailures != null ? this.consecutiveFailures : 0) + 1;
        this.consecutiveFailures = failures;

        if (failures >= degradedThreshold
                && this.healthStatus != IntegrationHealthStatus.TOKEN_EXPIRED
                && this.healthStatus != IntegrationHealthStatus.REAUTH_REQUIRED) {
            this.healthStatus = IntegrationHealthStatus.DEGRADED;
        }
    }

    public void markDisconnectedHealth() {
        this.healthStatus = IntegrationHealthStatus.DISCONNECTED;
        this.reauthRequired = false;
        this.lastHealthCheckAt = LocalDateTime.now();
    }

    public void markConnectedHealth() {
        this.healthStatus = IntegrationHealthStatus.CONNECTED;
        this.reauthRequired = false;
        this.consecutiveFailures = 0;
        this.lastError = null;
        this.lastValidatedAt = LocalDateTime.now();
        this.lastHealthCheckAt = LocalDateTime.now();
    }
}
