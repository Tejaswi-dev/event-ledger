package com.eventledger.gateway.controller;

import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.service.EventResult;
import com.eventledger.gateway.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> submitEvent(@Valid @RequestBody EventRequest request) {
        EventResult result = eventService.submitEvent(request);
        HttpStatus status = result.isCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.getResponse());
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable String eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getEventsByAccount(@RequestParam String account) {
        return ResponseEntity.ok(eventService.getEventsByAccount(account));
    }
}
