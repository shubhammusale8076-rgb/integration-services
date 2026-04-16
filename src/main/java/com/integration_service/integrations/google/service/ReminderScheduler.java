package com.integration_service.integrations.google.service;

import com.integration_service.dto.EventRequest;
import com.integration_service.integrations.google.entity.TrialBooking;
import com.integration_service.repository.TrialBookingRepo;
import com.integration_service.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private final TrialBookingRepo repository;
    private final EventService eventService;

    @Scheduled(fixedRate = 60000)
    public void sendReminders() {

        List<TrialBooking> bookings =
                repository.findByReminderTimeBeforeAndReminderSentFalse(LocalDateTime.now());

        for (TrialBooking booking : bookings) {

            EventRequest event = new EventRequest();
            event.setEventType("TRIAL_REMINDER");

            event.setData(Map.of(
                    "phone", booking.getClientPhone(),
                    "email", booking.getClientEmail(),
                    "name", booking.getClientName(),
                    "time", booking.getStartTime().toString()
            ));

            eventService.processEvent(event);

            booking.setReminderSent(true);
            repository.save(booking);
        }
    }
}
