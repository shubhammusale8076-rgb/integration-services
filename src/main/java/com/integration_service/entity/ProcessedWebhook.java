package com.integration_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_webhooks")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProcessedWebhook {

    @Id
    private String eventId;

    private String provider;

    private LocalDateTime processedAt;
}
