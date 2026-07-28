package com.example.monitoring.transaction.service;

import com.example.monitoring.rule.service.RuleEngineService;
import com.example.monitoring.transaction.dto.CreateTransactionRequest;
import com.example.monitoring.transaction.dto.GenerateTransactionsRequest;
import com.example.monitoring.transaction.entity.Transaction;
import com.example.monitoring.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TransactionService {

    private static final BigDecimal DEFAULT_MIN_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal DEFAULT_MAX_AMOUNT = new BigDecimal("25000.00");
    private static final int DEFAULT_STEP_SECONDS = 60;

    private static final String[] TRANSACTION_TYPES = {"DEBIT", "CREDIT"};
    private static final String[] DESCRIPTIONS = {
            "ATM withdrawal",
            "Online shopping",
            "Wire transfer",
            "Utility payment",
            "Subscription renewal"
    };

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
        return createTransaction(request, LocalDateTime.now());
    }

    public Transaction createTransaction(CreateTransactionRequest request, LocalDateTime createdAt) {
        Transaction transaction = new Transaction(
                request.getAccountId(),
                request.getPayeeId(),
                request.getAmount(),
                request.getCurrency() != null ? request.getCurrency() : "USD",
                request.getTransactionType(),
                request.getDescription(),
                createdAt
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

    /**
     * 自动生成测试交易数据（会触发规则评估并可能生成告警）。
     */
    public List<Transaction> generateMockTransactions(int count) {
        GenerateTransactionsRequest request = new GenerateTransactionsRequest();
        request.setCount(count);
        return generateMockTransactions(request);
    }

    public List<Transaction> generateMockTransactions(GenerateTransactionsRequest request) {
        GenerateTransactionsRequest options = request != null ? request : new GenerateTransactionsRequest();
        int count = options.getCount() != null ? options.getCount() : 100;
        validateGenerateRequest(options, count);

        BigDecimal minAmount = options.getMinAmount() != null ? options.getMinAmount() : DEFAULT_MIN_AMOUNT;
        BigDecimal maxAmount = options.getMaxAmount() != null ? options.getMaxAmount() : DEFAULT_MAX_AMOUNT;
        List<LocalDateTime> createdAtValues = buildCreatedAtValues(options, count);

        List<Transaction> generated = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            CreateTransactionRequest createRequest = new CreateTransactionRequest();
            createRequest.setAccountId("ACC-" + String.format("%03d", ThreadLocalRandom.current().nextInt(1, 21)));
            createRequest.setPayeeId("PAY-" + String.format("%03d", ThreadLocalRandom.current().nextInt(1, 51)));
            createRequest.setAmount(randomAmount(minAmount, maxAmount));
            createRequest.setCurrency("USD");
            createRequest.setTransactionType(TRANSACTION_TYPES[ThreadLocalRandom.current().nextInt(TRANSACTION_TYPES.length)]);
            createRequest.setDescription(DESCRIPTIONS[ThreadLocalRandom.current().nextInt(DESCRIPTIONS.length)]);
            generated.add(createTransaction(createRequest, createdAtValues.get(i)));
        }
        return generated;
    }

    private void validateGenerateRequest(GenerateTransactionsRequest request, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }

        BigDecimal minAmount = request.getMinAmount() != null ? request.getMinAmount() : DEFAULT_MIN_AMOUNT;
        BigDecimal maxAmount = request.getMaxAmount() != null ? request.getMaxAmount() : DEFAULT_MAX_AMOUNT;

        if (minAmount.signum() <= 0) {
            throw new IllegalArgumentException("minAmount must be greater than 0");
        }
        if (maxAmount.signum() <= 0) {
            throw new IllegalArgumentException("maxAmount must be greater than 0");
        }
        if (minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("minAmount must not be greater than maxAmount");
        }

        LocalDateTime startAt = request.getStartAt();
        LocalDateTime endAt = request.getEndAt();
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new IllegalArgumentException("startAt must not be after endAt");
        }

        if (request.getStepSeconds() != null && request.getStepSeconds() <= 0) {
            throw new IllegalArgumentException("stepSeconds must be greater than 0");
        }

        if (startAt != null && endAt != null) {
            long rangeSeconds = Duration.between(startAt, endAt).getSeconds();
            if (request.getStepSeconds() != null) {
                long requiredSeconds = (long) request.getStepSeconds() * Math.max(0, count - 1);
                if (requiredSeconds > rangeSeconds) {
                    throw new IllegalArgumentException("time range must be large enough for count and stepSeconds");
                }
            } else if (count > 1 && rangeSeconds < count - 1L) {
                throw new IllegalArgumentException("time range must span at least count - 1 seconds");
            }
        }
    }

    private List<LocalDateTime> buildCreatedAtValues(GenerateTransactionsRequest request, int count) {
        int stepSeconds = request.getStepSeconds() != null ? request.getStepSeconds() : DEFAULT_STEP_SECONDS;
        LocalDateTime startAt = request.getStartAt();
        LocalDateTime endAt = request.getEndAt();

        if (startAt == null && endAt == null) {
            LocalDateTime base = LocalDateTime.now().minusSeconds((long) stepSeconds * Math.max(0, count - 1));
            return buildSequentialCreatedAtValues(base, count, stepSeconds);
        }

        if (startAt != null && endAt == null) {
            return buildSequentialCreatedAtValues(startAt, count, stepSeconds);
        }

        if (startAt == null) {
            LocalDateTime base = endAt.minusSeconds((long) stepSeconds * Math.max(0, count - 1));
            return buildSequentialCreatedAtValues(base, count, stepSeconds);
        }

        if (request.getStepSeconds() != null || count == 1) {
            return buildSequentialCreatedAtValues(startAt, count, stepSeconds);
        }

        long totalSeconds = Duration.between(startAt, endAt).getSeconds();
        List<LocalDateTime> createdAtValues = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long offsetSeconds = (i * totalSeconds) / (count - 1L);
            createdAtValues.add(startAt.plusSeconds(offsetSeconds));
        }
        return createdAtValues;
    }

    private List<LocalDateTime> buildSequentialCreatedAtValues(LocalDateTime startAt, int count, int stepSeconds) {
        List<LocalDateTime> createdAtValues = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            createdAtValues.add(startAt.plusSeconds((long) stepSeconds * i));
        }
        return createdAtValues;
    }

    private BigDecimal randomAmount(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount.compareTo(maxAmount) == 0) {
            return minAmount.setScale(2, RoundingMode.HALF_UP);
        }
        double value = ThreadLocalRandom.current().nextDouble(minAmount.doubleValue(), maxAmount.doubleValue());
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
