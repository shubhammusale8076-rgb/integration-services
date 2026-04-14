package com.integration_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String tenantId;

    private String clientName;
    private String clientEmail;
    private String clientPhone;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String calendarEventId;

    private String status; // BOOKED, CANCELLED

    private LocalDateTime reminderTime;
    private boolean reminderSent;

    private LocalDateTime createdAt;
}
