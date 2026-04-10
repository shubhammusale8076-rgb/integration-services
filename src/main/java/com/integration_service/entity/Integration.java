package com.integration_service.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "integrations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Integration {

    @Id
    @GeneratedValue
    private UUID id;

    private String gymId;

    private String provider; // RAZORPAY, WHATSAPP, GOOGLE

    private Boolean connected;

    private String authType; // OAUTH, API_KEY
    private String status;   // CONNECTED, FAILED, PENDING

    @Column(columnDefinition = "TEXT")
    private String configJson; // encrypted API keys, tokens

    private LocalDateTime createdAt;

    private String tenantId;
}
