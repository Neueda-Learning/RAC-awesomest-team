package com.example.monitoring.transaction.service;

import com.example.monitoring.rule.service.RuleEngineService;
import com.example.monitoring.transaction.dto.CreateTransactionRequest;
import com.example.monitoring.transaction.entity.Transaction;
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
        Transaction transaction = new Transaction(
                request.getAccountId(),
                request.getPayeeId(),
                request.getAmount(),
                request.getCurrency() != null ? request.getCurrency() : "USD",
                request.getTransactionType(),
                request.getDescription(),
                LocalDateTime.now()
        );
        Transaction saved = transactionRepository.save(transaction);

        // 规则引擎评估（同步执行）
        ruleEngineService.evaluate(saved);

        return saved;
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
}
