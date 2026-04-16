package com.integration_service.controller;

import com.integration_service.dto.EventRequest;
import com.integration_service.service.event.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/integration/events")
@RequiredArgsConstructor
public class IntegrationEventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<String> receiveEvent(@Valid @RequestBody EventRequest request) {
        eventService.processEvent(request);
        return ResponseEntity.ok("Event received and processing started");
    }
}
