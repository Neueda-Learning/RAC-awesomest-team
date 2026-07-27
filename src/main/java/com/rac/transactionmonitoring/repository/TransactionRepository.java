package com.rac.transactionmonitoring.repository;

import com.rac.transactionmonitoring.model.Transaction;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {

    List<Transaction> findByAccountId(String accountId);

    List<Transaction> findByAccountIdAndPayeeId(String accountId, String payeeId);

    // 用于 Velocity 规则：统计某账户在时间窗口内的交易数
    @Query("SELECT COUNT(*) FROM transaction WHERE account_id = :accountId AND created_at >= :since")
    int countByAccountIdAndCreatedAtAfter(@Param("accountId") String accountId, @Param("since") LocalDateTime since);

    // 用于 Daily Limit 规则：统计某账户今日累计金额
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transaction WHERE account_id = :accountId AND created_at >= :startOfDay")
    java.math.BigDecimal sumAmountByAccountIdAndCreatedAtAfter(@Param("accountId") String accountId, @Param("startOfDay") LocalDateTime startOfDay);

    // 用于 New Payee 规则：查询某账户是否曾经向该收款方转账（排除当前交易）
    @Query("SELECT COUNT(*) FROM transaction WHERE account_id = :accountId AND payee_id = :payeeId AND id != :excludeId")
    int countPreviousTransactionsToPayee(@Param("accountId") String accountId, @Param("payeeId") String payeeId, @Param("excludeId") Long excludeId);
}

