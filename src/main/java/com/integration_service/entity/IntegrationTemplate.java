package com.integration_service.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "integration_templates")
@Getter
@Setter
public class IntegrationTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    private String provider; // RAZORPAY, WHATSAPP, GOOGLE

    private String displayName;

    private String authType; // OAUTH, API_KEY

    @Column(columnDefinition = "TEXT")
    private String configSchema; // JSON schema for form

    private Boolean active;
}
