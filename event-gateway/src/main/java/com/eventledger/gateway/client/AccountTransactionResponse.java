package com.eventledger.gateway.client;

import java.math.BigDecimal;

public class AccountTransactionResponse {

    private String eventId;
    private String accountId;
    private BigDecimal balance;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
