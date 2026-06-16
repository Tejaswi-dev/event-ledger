package com.eventledger.account.service;

import com.eventledger.account.dto.AccountResponse;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionRequest;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.dto.TransactionView;
import com.eventledger.account.entity.Account;
import com.eventledger.account.entity.Transaction;
import com.eventledger.account.exception.AccountNotFoundException;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResult applyTransaction(String accountId, TransactionRequest request) {
        Optional<Transaction> existing = transactionRepository.findById(request.getEventId());
        if (existing.isPresent()) {
            Transaction tx = existing.get();
            boolean samePayload = tx.getAccountId().equals(accountId)
                    && tx.getType().equals(request.getType())
                    && tx.getAmount().compareTo(request.getAmount()) == 0;
            if (!samePayload) {
                log.warn("Duplicate eventId {} received with different payload; returning original", request.getEventId());
            }
            BigDecimal balance = transactionRepository.calculateBalance(tx.getAccountId());
            return new TransactionResult(
                    new TransactionResponse(tx.getEventId(), tx.getAccountId(), balance), false);
        }

        Instant now = Instant.now();
        accountRepository.findById(accountId)
                .orElseGet(() -> accountRepository.save(new Account(accountId, now)));

        Transaction transaction = new Transaction(
                request.getEventId(), accountId, request.getType(),
                request.getAmount(), request.getEventTimestamp(), now);
        transactionRepository.save(transaction);

        BigDecimal balance = transactionRepository.calculateBalance(accountId);
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.setBalance(balance);
        account.setUpdatedAt(now);
        accountRepository.save(account);

        return new TransactionResult(
                new TransactionResponse(transaction.getEventId(), accountId, balance), true);
    }

    public BalanceResponse getBalance(String accountId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        BigDecimal balance = transactionRepository.calculateBalance(accountId);
        return new BalanceResponse(accountId, balance);
    }

    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        List<TransactionView> transactions = transactionRepository
                .findByAccountIdOrderByEventTimestampDesc(accountId)
                .stream()
                .map(t -> new TransactionView(t.getEventId(), t.getType(), t.getAmount(), t.getEventTimestamp()))
                .collect(Collectors.toList());
        BigDecimal balance = transactionRepository.calculateBalance(accountId);
        return new AccountResponse(accountId, balance, account.getCreatedAt(), transactions);
    }
}
