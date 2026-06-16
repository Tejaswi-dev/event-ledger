package com.eventledger.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AccountResponse {

    private final String accountId;
    private final BigDecimal balance;
    private final Instant createdAt;
    private final List<TransactionView> transactions;

    public AccountResponse(String accountId, BigDecimal balance, Instant createdAt, List<TransactionView> transactions) {
        this.accountId = accountId;
        this.balance = balance;
        this.createdAt = createdAt;
        this.transactions = transactions;
    }

    public String getAccountId() { return accountId; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
    public List<TransactionView> getTransactions() { return transactions; }
}
