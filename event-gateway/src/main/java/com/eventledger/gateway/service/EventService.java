package com.eventledger.gateway.service;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.client.AccountTransactionResult;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.entity.Event;
import com.eventledger.gateway.exception.EventNotFoundException;
import com.eventledger.gateway.repository.EventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final AccountServiceClient accountServiceClient;
    private final ObjectMapper objectMapper;

    public EventService(EventRepository eventRepository, AccountServiceClient accountServiceClient,
                         ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.accountServiceClient = accountServiceClient;
        this.objectMapper = objectMapper;
    }

    public EventResult submitEvent(EventRequest request) {
        AccountTransactionResult txResult = accountServiceClient.applyTransaction(request);

        if (!txResult.isCreated()) {
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new EventNotFoundException(request.getEventId()));
            return new EventResult(toResponse(event), false);
        }

        Event event = new Event(
                request.getEventId(), request.getAccountId(), request.getType(),
                request.getAmount(), request.getCurrency(), request.getEventTimestamp(),
                writeMetadata(request.getMetadata()), Instant.now());
        eventRepository.save(event);
        return new EventResult(toResponse(event), true);
    }

    public EventResponse getEvent(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        return toResponse(event);
    }

    public List<EventResponse> getEventsByAccount(String accountId) {
        return eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getEventId(), event.getAccountId(), event.getType(), event.getAmount(),
                event.getCurrency(), event.getEventTimestamp(), readMetadata(event.getMetadata()),
                event.getReceivedAt());
    }

    private String writeMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid metadata", e);
        }
    }

    private Map<String, Object> readMetadata(String metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return objectMapper.readValue(metadata, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt metadata in storage", e);
        }
    }
}
