package com.eventledger.gateway.service;

import com.eventledger.gateway.dto.EventResponse;

public class EventResult {

    private final EventResponse response;
    private final boolean created;

    public EventResult(EventResponse response, boolean created) {
        this.response = response;
        this.created = created;
    }

    public EventResponse getResponse() { return response; }
    public boolean isCreated() { return created; }
}
