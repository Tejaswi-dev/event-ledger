package com.eventledger.gateway.client;

public class AccountTransactionResult {

    private final AccountTransactionResponse response;
    private final boolean created;

    public AccountTransactionResult(AccountTransactionResponse response, boolean created) {
        this.response = response;
        this.created = created;
    }

    public AccountTransactionResponse getResponse() { return response; }
    public boolean isCreated() { return created; }
}
