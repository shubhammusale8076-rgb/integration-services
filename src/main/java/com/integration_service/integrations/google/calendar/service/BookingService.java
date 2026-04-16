package com.integration_service.integrations.google.calendar.service;

import com.integration_service.integrations.google.entity.TrialBooking;
import com.integration_service.repository.TrialBookingRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final TrialBookingRepo repository;

    public void validateSlot(String tenantId, LocalDateTime start, LocalDateTime end) {

        List<TrialBooking> bookings =
                repository.findByTenantIdAndStartTimeBetween(tenantId, start, end);

        if (!bookings.isEmpty()) {
            throw new RuntimeException("Slot already booked");
        }
    }

    public TrialBooking saveBooking(
            String tenantId,
            String name,
            String email,
            String phone,
            LocalDateTime start,
            LocalDateTime end,
            String eventId
    ) {

        return repository.save(
                TrialBooking.builder()
                        .tenantId(tenantId)
                        .clientName(name)
                        .clientEmail(email)
                        .clientPhone(phone)
                        .startTime(start)
                        .endTime(end)
                        .calendarEventId(eventId)
                        .status("BOOKED")
                        .reminderTime(start.minusHours(1))
                        .reminderSent(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
}