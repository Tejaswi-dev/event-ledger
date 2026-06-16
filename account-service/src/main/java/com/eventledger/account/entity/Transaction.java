package com.eventledger.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Transaction() {}

    public Transaction(String eventId, String accountId, String type,
                       BigDecimal amount, Instant eventTimestamp, Instant createdAt) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.eventTimestamp = eventTimestamp;
        this.createdAt = createdAt;
    }

    public String getEventId() { return eventId; }
    public String getAccountId() { return accountId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public Instant getCreatedAt() { return createdAt; }
}
