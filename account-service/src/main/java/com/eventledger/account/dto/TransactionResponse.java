package com.eventledger.account.dto;

import java.math.BigDecimal;

public class TransactionResponse {

    private final String eventId;
    private final String accountId;
    private final BigDecimal balance;

    public TransactionResponse(String eventId, String accountId, BigDecimal balance) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.balance = balance;
    }

    public String getEventId() { return eventId; }
    public String getAccountId() { return accountId; }
    public BigDecimal getBalance() { return balance; }
}
