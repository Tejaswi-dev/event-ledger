package com.eventledger.gateway;

import com.eventledger.account.AccountServiceApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Boots a real, separate Account Service instance (not mocked) and drives the
// Gateway through MockMvc, exercising the full Gateway -> Account Service flow.
@SpringBootTest
@AutoConfigureMockMvc
class EventGatewayEndToEndTest {

    private static ConfigurableApplicationContext accountServiceContext;

    @DynamicPropertySource
    static void registerAccountServiceUrl(DynamicPropertyRegistry registry) {
        accountServiceContext = new SpringApplicationBuilder(AccountServiceApplication.class)
                .initializers(new ServerPortInfoApplicationContextInitializer())
                .properties(
                        "server.port=0",
                        "spring.datasource.url=jdbc:h2:mem:e2e-accountdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .run();
        Integer port = accountServiceContext.getEnvironment().getProperty("local.server.port", Integer.class);
        registry.add("account-service.base-url", () -> "http://localhost:" + port);
    }

    @AfterAll
    static void stopAccountService() {
        accountServiceContext.close();
    }

    @Autowired
    private MockMvc mockMvc;

    // Full round trip against a real, separately booted Account Service (not mocked)
    @Test
    void appliesTransactionEndToEnd() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .content(eventJson("evt-e2e-1", "acct-e2e-1", "CREDIT", "100.00", "2026-05-15T10:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-e2e-1"))
                .andExpect(jsonPath("$.accountId").value("acct-e2e-1"));
    }

    // Idempotency holds across two real, separately running services, not just mocks
    @Test
    void duplicateIsIdempotentEndToEnd() throws Exception {
        String body = eventJson("evt-e2e-2", "acct-e2e-2", "CREDIT", "50.00", "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events").contentType("application/json").content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events").contentType("application/json").content(body))
                .andExpect(status().isOk());
    }

    private String eventJson(String eventId, String accountId, String type, String amount, String timestamp) {
        return String.format(
                "{\"eventId\":\"%s\",\"accountId\":\"%s\",\"type\":\"%s\",\"amount\":%s,\"currency\":\"USD\"," +
                        "\"eventTimestamp\":\"%s\"}",
                eventId, accountId, type, amount, timestamp);
    }
}
