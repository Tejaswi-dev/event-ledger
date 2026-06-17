package com.eventledger.gateway.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResiliencyAndTracePropagationTest {

    private static MockWebServer mockWebServer;

    @DynamicPropertySource
    static void registerAccountServiceUrl(DynamicPropertyRegistry registry) {
        try {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        registry.add("account-service.base-url", () -> "http://localhost:" + mockWebServer.getPort());
    }

    @AfterAll
    static void shutdownServer() throws IOException {
        mockWebServer.shutdown();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    // Circuit breaker state persists across test methods in this shared context, so reset before every test
    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("accountService").reset();
    }

    // Sliding window is 10 calls, failure-rate-threshold is 50% -> 10 failures opens the circuit
    @Test
    void circuitOpensOnFailures() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            mockMvc.perform(post("/events")
                    .contentType("application/json")
                    .content(eventJson("evt-cb-" + i, "acct-cb", "CREDIT", "10.00", "2026-05-15T10:00:00Z")));
        }
        int requestsBeforeOpen = mockWebServer.getRequestCount();

        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content(eventJson("evt-cb-final", "acct-cb", "CREDIT", "10.00", "2026-05-15T10:00:00Z")))
                .andExpect(status().isServiceUnavailable());

        // Circuit was open, so this last call should never have reached the server
        assertThat(mockWebServer.getRequestCount()).isEqualTo(requestsBeforeOpen);
    }

    // GET endpoints only read the Gateway's own database, so they must keep working even if
    // Account Service never responds
    @Test
    void getsDegradeGracefully() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"eventId\":\"evt-degrade-1\",\"accountId\":\"acct-degrade\",\"balance\":10.00}")
                .addHeader("Content-Type", "application/json"));
        mockMvc.perform(post("/events")
                .contentType("application/json")
                .content(eventJson("evt-degrade-1", "acct-degrade", "CREDIT", "10.00", "2026-05-15T10:00:00Z")));

        mockMvc.perform(get("/events/evt-degrade-1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/events").param("account", "acct-degrade"))
                .andExpect(status().isOk());
    }

    // Verifies the actual outgoing HTTP request carries a valid W3C traceparent header
    @Test
    void sendsTraceparentHeader() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"eventId\":\"evt-trace-1\",\"accountId\":\"acct-trace\",\"balance\":10.00}")
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(post("/events")
                .contentType("application/json")
                .content(eventJson("evt-trace-1", "acct-trace", "CREDIT", "10.00", "2026-05-15T10:00:00Z")));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getHeader("traceparent")).matches("00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]");
    }

    private String eventJson(String eventId, String accountId, String type, String amount, String timestamp) {
        return String.format(
                "{\"eventId\":\"%s\",\"accountId\":\"%s\",\"type\":\"%s\",\"amount\":%s,\"currency\":\"USD\"," +
                        "\"eventTimestamp\":\"%s\"}",
                eventId, accountId, type, amount, timestamp);
    }
}
