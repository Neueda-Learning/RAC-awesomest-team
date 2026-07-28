package com.example.monitoring.transaction.service;

import com.example.monitoring.rule.service.RuleEngineService;
import com.example.monitoring.transaction.dto.CreateTransactionRequest;
import com.example.monitoring.transaction.dto.GenerateTransactionsRequest;
import com.example.monitoring.transaction.entity.Transaction;
import com.example.monitoring.transaction.entity.TransactionType;
import com.example.monitoring.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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

    private static final String[] ACCOUNTS = {"ACC-001", "ACC-002", "ACC-003", "ACC-004", "ACC-005"};
    private static final String[] PAYEES   = {"ACC-010", "ACC-011", "ACC-012", "PAYEE-A", "PAYEE-B", "PAYEE-C"};
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "CNY"};
    // 只生成需要触发规则的类型（不含 SALARY/REFUND 豁免类型）
    private static final TransactionType[] GEN_TYPES = {
        TransactionType.TRANSFER_OUT, TransactionType.DEPOSIT, TransactionType.WITHDRAWAL
    };

    /**
     * 批量生成模拟交易数据，用于测试规则引擎。
     *
     * @param request 生成参数（数量、金额范围、时间范围、时间步长）
     * @return 已保存并触发规则评估的交易列表
     * @throws IllegalArgumentException 如果参数不合法
     */
    public List<Transaction> generateMockTransactions(GenerateTransactionsRequest request) {
        int count = request.getCount() != null ? request.getCount() : 100;
        if (count <= 0 || count > 10000) {
            throw new IllegalArgumentException("count must be between 1 and 10000");
        }

        BigDecimal minAmount = request.getMinAmount() != null
                ? request.getMinAmount() : BigDecimal.valueOf(10);
        BigDecimal maxAmount = request.getMaxAmount() != null
                ? request.getMaxAmount() : BigDecimal.valueOf(20000);
        if (minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("minAmount must not be greater than maxAmount");
        }

        LocalDateTime startAt = request.getStartAt() != null
                ? request.getStartAt() : LocalDateTime.now().minusDays(7);
        LocalDateTime endAt = request.getEndAt() != null
                ? request.getEndAt() : LocalDateTime.now();
        if (startAt.isAfter(endAt)) {
            throw new IllegalArgumentException("startAt must not be after endAt");
        }

        // 时间步长（秒），默认平均分布在时间区间内
        long totalSeconds = java.time.Duration.between(startAt, endAt).getSeconds();
        long stepSeconds = request.getStepSeconds() != null
                ? request.getStepSeconds()
                : Math.max(1, totalSeconds / count);

        Random random = new Random();
        List<Transaction> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            TransactionType txType = GEN_TYPES[random.nextInt(GEN_TYPES.length)];
            String accountId = ACCOUNTS[random.nextInt(ACCOUNTS.length)];

            // TRANSFER_OUT 需要 payeeId，DEPOSIT/WITHDRAWAL 不需要
            String payeeId = txType.requiresPayee()
                    ? PAYEES[random.nextInt(PAYEES.length)]
                    : null;

            // 在金额范围内随机生成
            double range = maxAmount.subtract(minAmount).doubleValue();
            BigDecimal amount = minAmount.add(
                    BigDecimal.valueOf(random.nextDouble() * range)
            ).setScale(2, RoundingMode.HALF_UP);

            String currency = CURRENCIES[random.nextInt(CURRENCIES.length)];

            // 在时间区间内按步长递增
            LocalDateTime createdAt = startAt.plusSeconds((long) i * stepSeconds);
            if (createdAt.isAfter(endAt)) {
                createdAt = endAt;
            }

            Transaction tx = new Transaction(
                    accountId, payeeId, amount, currency,
                    txType.name(), "Auto-generated #" + (i + 1), createdAt
            );
            Transaction saved = transactionRepository.save(tx);
            ruleEngineService.evaluate(saved);
            result.add(saved);
        }

        return result;
    }
}
