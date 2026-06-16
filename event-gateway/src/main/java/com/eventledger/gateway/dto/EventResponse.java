package com.eventledger.gateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class EventResponse {

    private final String eventId;
    private final String accountId;
    private final String type;
    private final BigDecimal amount;
    private final String currency;
    private final Instant eventTimestamp;
    private final Map<String, Object> metadata;
    private final Instant receivedAt;

    public EventResponse(String eventId, String accountId, String type, BigDecimal amount,
                          String currency, Instant eventTimestamp, Map<String, Object> metadata,
                          Instant receivedAt) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.eventTimestamp = eventTimestamp;
        this.metadata = metadata;
        this.receivedAt = receivedAt;
    }

    public String getEventId() { return eventId; }
    public String getAccountId() { return accountId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getReceivedAt() { return receivedAt; }
}
