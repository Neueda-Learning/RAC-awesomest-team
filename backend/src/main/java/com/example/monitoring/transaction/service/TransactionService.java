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
import java.util.*;
import java.util.stream.StreamSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RuleEngineService ruleEngineService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openexchangerates.apiId:da4ca83d160f44168d996e6e3fed6f79}")
    @SuppressWarnings("unused")
    private String fxApiId;

    private static final String FX_API_URL = "https://openexchangerates.org/api/latest.json";
    private static final long FX_CACHE_TTL_MS = 5 * 60 * 1000; // 5分钟缓存
    private Map<String, Object> fxCache = null;
    private long fxCacheTime = 0;

    public TransactionService(TransactionRepository transactionRepository,
                              RuleEngineService ruleEngineService,
                              RestTemplate restTemplate,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.ruleEngineService = ruleEngineService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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
        return StreamSupport
                .stream(transactionRepository.findAll().spliterator(), false)
                .collect(java.util.stream.Collectors.toList());
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
     * 按金额范围和/或日期区间筛选（支持汇率换算为USD进行比较）
     * minAmount/maxAmount 单位为 USD，系统会自动将交易转换为 USD 后进行比较
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

        // 先获取符合日期条件的交易
        List<Transaction> candidates;
        if (from != null && to != null) {
            candidates = transactionRepository.findByCreatedAtBetween(from, to);
        } else {
            candidates = new ArrayList<>();
            transactionRepository.findAll().forEach(candidates::add);
        }

        // 如果没有金额条件，直接返回
        if (minAmount == null || maxAmount == null) {
            return candidates;
        }

        // 获取汇率并按USD范围过滤
        Map<String, Object> ratesData = getFxRates();
        if (ratesData == null || ratesData.isEmpty()) {
            // 汇率获取失败，降级为按原币种过滤（仅USD）
            return candidates.stream()
                    .filter(tx -> "USD".equalsIgnoreCase(tx.getCurrency()) &&
                            tx.getAmount().compareTo(minAmount) >= 0 &&
                            tx.getAmount().compareTo(maxAmount) <= 0)
                    .toList();
        }

        // 用汇率进行转换过滤
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> rates = (Map<String, BigDecimal>) ratesData.get("rates");
        return candidates.stream()
                .filter(tx -> {
                    BigDecimal amountInUsd = convertToUsd(tx.getAmount(), tx.getCurrency(), rates);
                    return amountInUsd != null &&
                            amountInUsd.compareTo(minAmount) >= 0 &&
                            amountInUsd.compareTo(maxAmount) <= 0;
                })
                .toList();
    }

    /**
     * 获取汇率数据（带缓存，5分钟TTL）
     */
    private Map<String, Object> getFxRates() {
        long now = System.currentTimeMillis();
        if (fxCache != null && (now - fxCacheTime) < FX_CACHE_TTL_MS) {
            return fxCache;
        }

        try {
            String url = FX_API_URL + "?app_id=" + fxApiId;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            
            if (jsonNode.has("rates")) {
                JsonNode ratesNode = jsonNode.get("rates");
                Map<String, BigDecimal> rates = new HashMap<>();
                ratesNode.fieldNames().forEachRemaining(currency ->
                    rates.put(currency, new BigDecimal(ratesNode.get(currency).asText()))
                );

                fxCache = new HashMap<>();
                fxCache.put("rates", rates);
                fxCacheTime = now;
                return fxCache;
            }
        } catch (Exception e) {
            // 日志或其他处理，继续返回缓存或null
            System.err.println("Failed to fetch FX rates: " + e.getMessage());
        }
        return fxCache; // 如果失败，返回缓存或null
    }

    /**
     * 将指定币种的金额转换为 USD
     */
    private BigDecimal convertToUsd(BigDecimal amount, String currency, Map<String, BigDecimal> rates) {
        if (amount == null || amount.signum() == 0) {
            return amount;
        }
        String currencyCode = (currency != null ? currency : "USD").toUpperCase();
        if ("USD".equals(currencyCode)) {
            return amount;
        }
        BigDecimal rate = rates.get(currencyCode);
        if (rate == null || rate.signum() <= 0) {
            return null;
        }
        return amount.divide(rate, 2, RoundingMode.HALF_UP);
    }

    private static final String[] ACCOUNTS = {"ACC-001", "ACC-002", "ACC-003", "ACC-004", "ACC-005"};
    private static final String[] PAYEES   = {"ACC-010", "ACC-011", "ACC-012", "PAYEE-A", "PAYEE-B", "PAYEE-C"};
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "CNY", "JPY", "AUD", "CAD", "HKD", "SGD", "CHF"};
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
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }
        if (count > 10000) {
            throw new IllegalArgumentException("count must not exceed 10000");
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
