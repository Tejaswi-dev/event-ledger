package com.eventledger.account.service;

import com.eventledger.account.dto.TransactionResponse;

public class TransactionResult {

    private final TransactionResponse response;
    private final boolean created;

    public TransactionResult(TransactionResponse response, boolean created) {
        this.response = response;
        this.created = created;
    }

    public TransactionResponse getResponse() { return response; }
    public boolean isCreated() { return created; }
}
