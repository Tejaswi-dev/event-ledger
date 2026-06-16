package com.eventledger.gateway.client;

import java.math.BigDecimal;
import java.time.Instant;

public class ApplyTransactionRequest {

    private final String eventId;
    private final String type;
    private final BigDecimal amount;
    private final Instant eventTimestamp;

    public ApplyTransactionRequest(String eventId, String type, BigDecimal amount, Instant eventTimestamp) {
        this.eventId = eventId;
        this.type = type;
        this.amount = amount;
        this.eventTimestamp = eventTimestamp;
    }

    public String getEventId() { return eventId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Instant getEventTimestamp() { return eventTimestamp; }
}
