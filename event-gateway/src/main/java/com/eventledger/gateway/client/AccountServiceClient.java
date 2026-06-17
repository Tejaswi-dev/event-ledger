package com.eventledger.gateway.client;

import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AccountServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClient.class);

    private final RestClient restClient;

    public AccountServiceClient(RestClient accountServiceRestClient) {
        this.restClient = accountServiceRestClient;
    }

    @CircuitBreaker(name = "accountService")
    @Retry(name = "accountService", fallbackMethod = "applyTransactionFallback")
    public AccountTransactionResult applyTransaction(EventRequest request) {
        ApplyTransactionRequest body = new ApplyTransactionRequest(
                request.getEventId(), request.getType(), request.getAmount(), request.getEventTimestamp());

        ResponseEntity<AccountTransactionResponse> response = restClient.post()
                .uri("/accounts/{accountId}/transactions", request.getAccountId())
                .body(body)
                .retrieve()
                .toEntity(AccountTransactionResponse.class);

        boolean created = response.getStatusCode() == HttpStatus.CREATED;
        return new AccountTransactionResult(response.getBody(), created);
    }

    private AccountTransactionResult applyTransactionFallback(EventRequest request, Exception ex) {
        log.error("Account Service call failed for eventId {}: {}", request.getEventId(), ex.getMessage());
        throw new AccountServiceUnavailableException("Account Service is unavailable", ex);
    }
}
