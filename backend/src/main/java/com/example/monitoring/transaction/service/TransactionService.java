package com.example.monitoring.transaction.service;

import com.example.monitoring.rule.service.RuleEngineService;
import com.example.monitoring.transaction.dto.CreateTransactionRequest;
import com.example.monitoring.transaction.entity.Transaction;
import com.example.monitoring.transaction.entity.TransactionType;
import com.example.monitoring.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RuleEngineService ruleEngineService;

    public TransactionService(TransactionRepository transactionRepository,
                              RuleEngineService ruleEngineService) {
        this.transactionRepository = transactionRepository;
        this.ruleEngineService = ruleEngineService;
    }

    /**
     * 提交一笔交易，保存后自动触发规则引擎评估
     */
    public Transaction createTransaction(CreateTransactionRequest request) {
        TransactionType transactionType = TransactionType.from(request.getTransactionType());
        String normalizedPayeeId = normalizePayeeId(request.getPayeeId());
        validatePayeeRules(transactionType, normalizedPayeeId);

        Transaction transaction = new Transaction(
                request.getAccountId(),
                normalizedPayeeId,
                request.getAmount(),
                request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "USD",
                transactionType.name(),
                request.getDescription(),
                LocalDateTime.now()
        );
        Transaction saved = transactionRepository.save(transaction);

        // 规则引擎评估（同步执行）
        ruleEngineService.evaluate(saved);

        return saved;
    }

    private String normalizePayeeId(String payeeId) {
        if (payeeId == null) {
            return null;
        }
        String trimmed = payeeId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validatePayeeRules(TransactionType transactionType, String payeeId) {
        if (transactionType.requiresPayee() && payeeId == null) {
            throw new IllegalArgumentException("payeeId is required for transactionType " + transactionType.name());
        }
        if (transactionType.forbidsPayee() && payeeId != null) {
            throw new IllegalArgumentException("payeeId must be empty for transactionType " + transactionType.name());
        }
    }

    public List<Transaction> getAllTransactions() {
        return (List<Transaction>) transactionRepository.findAll();
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    public List<Transaction> getTransactionsByAccount(String accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    // 按描述关键词模糊搜索
    public List<Transaction> searchByDescription(String keyword) {
        return transactionRepository.searchByDescription("%" + keyword + "%");
    }

    /**
     * 按金额范围和/或日期区间筛选
     * 前端可以只传其中一组，也可以两组都传
     */
    public List<Transaction> filterTransactions(java.math.BigDecimal minAmount,
                                                java.math.BigDecimal maxAmount,
                                                LocalDateTime from,
                                                LocalDateTime to) {
        // 校验金额范围
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("minAmount must not be greater than maxAmount");
        }
        // 校验日期区间
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date");
        }
        boolean hasAmount = minAmount != null && maxAmount != null;
        boolean hasDate = from != null && to != null;

        if (hasAmount && hasDate) {
            return transactionRepository.findByAmountBetweenAndCreatedAtBetween(minAmount, maxAmount, from, to);
        } else if (hasAmount) {
            return transactionRepository.findByAmountBetween(minAmount, maxAmount);
        } else if (hasDate) {
            return transactionRepository.findByCreatedAtBetween(from, to);
        } else {
            return getAllTransactions();
        }
    }
}
