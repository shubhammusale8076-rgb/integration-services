package com.integration_service.repository;

import com.integration_service.entity.TrialBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrialBookingRepo extends JpaRepository<TrialBooking, String> {
    List<TrialBooking> findByTenantIdAndStartTimeBetween(
            String tenantId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<TrialBooking> findByReminderTimeBeforeAndReminderSentFalse(LocalDateTime now);
}
