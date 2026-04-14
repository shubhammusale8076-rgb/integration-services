package com.integration_service.service.googleService;

import java.util.List;
import java.util.Map;

public class CalendarEventBuilder {

    public static Map<String, Object> build(
            String summary,
            String description,
            String start,
            String end,
            String attendeeEmail
    ) {

        return Map.of(
                "summary", summary,
                "description", description,
                "start", Map.of(
                        "dateTime", start,
                        "timeZone", "Asia/Kolkata"
                ),
                "end", Map.of(
                        "dateTime", end,
                        "timeZone", "Asia/Kolkata"
                ),
                "attendees", List.of(
                        Map.of("email", attendeeEmail)
                )
        );
    }
}
