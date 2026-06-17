package com.eventledger.account.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // First time seeing this eventId -> 201 with the new balance
    @Test
    void createsTransaction() throws Exception {
        mockMvc.perform(post("/accounts/acct-1/transactions")
                        .contentType("application/json")
                        .content(transactionJson("evt-1", "CREDIT", "150.00", "2026-05-15T10:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.accountId").value("acct-1"))
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    // Same eventId submitted twice -> 200 with the original balance, not applied again
    @Test
    void duplicateEventIdIsIdempotent() throws Exception {
        String body = transactionJson("evt-2", "CREDIT", "100.00", "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/accounts/acct-2/transactions")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/acct-2/transactions")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    // Same eventId, different amount/type -> first write wins, original is returned unaltered
    @Test
    void duplicatePayloadMismatchKeepsOriginal() throws Exception {
        String original = transactionJson("evt-3", "CREDIT", "100.00", "2026-05-15T10:00:00Z");
        String conflicting = transactionJson("evt-3", "DEBIT", "999.00", "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/accounts/acct-3/transactions")
                        .contentType("application/json")
                        .content(original))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/acct-3/transactions")
                        .contentType("application/json")
                        .content(conflicting))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void invalidTypeRejected() throws Exception {
        mockMvc.perform(post("/accounts/acct-4/transactions")
                        .contentType("application/json")
                        .content(transactionJson("evt-4", "INVALID", "50.00", "2026-05-15T10:00:00Z")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void zeroAmountRejected() throws Exception {
        mockMvc.perform(post("/accounts/acct-5/transactions")
                        .contentType("application/json")
                        .content(transactionJson("evt-5", "CREDIT", "0", "2026-05-15T10:00:00Z")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingEventIdRejected() throws Exception {
        String body = "{\"type\":\"CREDIT\",\"amount\":50.00,\"eventTimestamp\":\"2026-05-15T10:00:00Z\"}";

        mockMvc.perform(post("/accounts/acct-6/transactions")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // Net balance = sum(CREDIT) - sum(DEBIT)
    @Test
    void netBalanceFromCreditsAndDebits() throws Exception {
        applyTransaction("acct-7", "evt-7a", "CREDIT", "200.00", "2026-05-15T10:00:00Z");
        applyTransaction("acct-7", "evt-7b", "DEBIT", "50.00", "2026-05-15T11:00:00Z");
        applyTransaction("acct-7", "evt-7c", "CREDIT", "25.00", "2026-05-15T12:00:00Z");

        mockMvc.perform(get("/accounts/acct-7/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(175.00));
    }

    // Balance is recomputed from scratch, so arrival order of eventTimestamps doesn't matter
    @Test
    void outOfOrderTimestampsDoNotAffectBalance() throws Exception {
        applyTransaction("acct-8", "evt-8a", "CREDIT", "100.00", "2026-05-15T12:00:00Z");
        applyTransaction("acct-8", "evt-8b", "DEBIT", "30.00", "2026-05-15T08:00:00Z");

        mockMvc.perform(get("/accounts/acct-8/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));
    }

    @Test
    void missingAccountReturns404() throws Exception {
        mockMvc.perform(get("/accounts/does-not-exist/balance"))
                .andExpect(status().isNotFound());
    }

    @Test
    void accountIncludesTransactions() throws Exception {
        applyTransaction("acct-9", "evt-9a", "CREDIT", "100.00", "2026-05-15T10:00:00Z");
        applyTransaction("acct-9", "evt-9b", "DEBIT", "40.00", "2026-05-15T11:00:00Z");

        mockMvc.perform(get("/accounts/acct-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(60.00))
                .andExpect(jsonPath("$.transactions", hasSize(2)));
    }

    private void applyTransaction(String accountId, String eventId, String type, String amount, String timestamp) throws Exception {
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType("application/json")
                .content(transactionJson(eventId, type, amount, timestamp)));
    }

    private String transactionJson(String eventId, String type, String amount, String timestamp) {
        return String.format(
                "{\"eventId\":\"%s\",\"type\":\"%s\",\"amount\":%s,\"eventTimestamp\":\"%s\"}",
                eventId, type, amount, timestamp);
    }
}
