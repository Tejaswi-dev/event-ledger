package com.eventledger.account.repository;

import com.eventledger.account.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByAccountIdOrderByEventTimestampAsc(String accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.type = 'CREDIT' THEN t.amount ELSE -t.amount END), 0) " +
           "FROM Transaction t WHERE t.accountId = :accountId")
    BigDecimal calculateBalance(@Param("accountId") String accountId);
}
