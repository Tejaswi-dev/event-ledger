package com.eventledger.gateway.controller;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.client.AccountTransactionResponse;
import com.eventledger.gateway.client.AccountTransactionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountServiceClient accountServiceClient;

    // Account Service reports the transaction as newly created -> Gateway stores the event, returns 201
    @Test
    void createsEvent() throws Exception {
        stubAccountService("evt-1", "acct-1", "150.00", true);

        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content(eventJson("evt-1", "acct-1", "CREDIT", "150.00", "2026-05-15T10:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-1"));
    }

    // Account Service reports the transaction as a duplicate -> Gateway returns its own stored copy with 200
    @Test
    void duplicateEventIdIsIdempotent() throws Exception {
        stubAccountService("evt-2", "acct-2", "75.00", true);
        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content(eventJson("evt-2", "acct-2", "CREDIT", "75.00", "2026-05-15T10:00:00Z")))
                .andExpect(status().isCreated());

        stubAccountService("evt-2", "acct-2", "75.00", false);
        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content(eventJson("evt-2", "acct-2", "CREDIT", "75.00", "2026-05-15T10:00:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-2"));
    }

    @Test
    void invalidCurrencyRejected() throws Exception {
        String body = "{\"eventId\":\"evt-3\",\"accountId\":\"acct-3\",\"type\":\"CREDIT\",\"amount\":10.00," +
                "\"currency\":\"EUR\",\"eventTimestamp\":\"2026-05-15T10:00:00Z\"}";

        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidTypeRejected() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content(eventJson("evt-4", "acct-4", "INVALID", "10.00", "2026-05-15T10:00:00Z")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void zeroAmountRejected() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content(eventJson("evt-5", "acct-5", "CREDIT", "0", "2026-05-15T10:00:00Z")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingAccountIdRejected() throws Exception {
        String body = "{\"eventId\":\"evt-6\",\"type\":\"CREDIT\",\"amount\":10.00,\"currency\":\"USD\"," +
                "\"eventTimestamp\":\"2026-05-15T10:00:00Z\"}";

        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchesEventById() throws Exception {
        stubAccountService("evt-7", "acct-7", "20.00", true);
        mockMvc.perform(post("/events")
                .contentType("application/json")
                .content(eventJson("evt-7", "acct-7", "CREDIT", "20.00", "2026-05-15T10:00:00Z")));

        mockMvc.perform(get("/events/evt-7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acct-7"));
    }

    @Test
    void missingEventReturns404() throws Exception {
        mockMvc.perform(get("/events/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    // Events submitted out of order must still come back sorted ascending by eventTimestamp
    @Test
    void accountEventsSortedChronologically() throws Exception {
        stubAccountService("evt-8b", "acct-8", "10.00", true);
        mockMvc.perform(post("/events")
                .contentType("application/json")
                .content(eventJson("evt-8b", "acct-8", "CREDIT", "10.00", "2026-05-15T12:00:00Z")));

        stubAccountService("evt-8a", "acct-8", "5.00", true);
        mockMvc.perform(post("/events")
                .contentType("application/json")
                .content(eventJson("evt-8a", "acct-8", "CREDIT", "5.00", "2026-05-15T08:00:00Z")));

        mockMvc.perform(get("/events").param("account", "acct-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt-8a"))
                .andExpect(jsonPath("$[1].eventId").value("evt-8b"));
    }

    private void stubAccountService(String eventId, String accountId, String balance, boolean created) {
        AccountTransactionResponse response = new AccountTransactionResponse();
        response.setEventId(eventId);
        response.setAccountId(accountId);
        response.setBalance(new BigDecimal(balance));
        when(accountServiceClient.applyTransaction(any()))
                .thenReturn(new AccountTransactionResult(response, created));
    }

    private String eventJson(String eventId, String accountId, String type, String amount, String timestamp) {
        return String.format(
                "{\"eventId\":\"%s\",\"accountId\":\"%s\",\"type\":\"%s\",\"amount\":%s,\"currency\":\"USD\"," +
                        "\"eventTimestamp\":\"%s\"}",
                eventId, accountId, type, amount, timestamp);
    }
}
