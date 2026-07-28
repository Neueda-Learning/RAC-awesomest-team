package com.example.monitoring.rule.service;

import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.rule.entity.MonitoringRule;
import com.example.monitoring.rule.entity.RuleCondition;
import com.example.monitoring.rule.repository.MonitoringRuleRepository;
import com.example.monitoring.rule.repository.RuleConditionRepository;
import com.example.monitoring.transaction.entity.Transaction;
import com.example.monitoring.transaction.entity.TransactionType;
import com.example.monitoring.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class RuleEngineService {

    private final MonitoringRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final RuleConditionRepository conditionRepository;

    public RuleEngineService(MonitoringRuleRepository ruleRepository,
                             TransactionRepository transactionRepository,
                             AlertRepository alertRepository,
                             RuleConditionRepository conditionRepository) {
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
        this.conditionRepository = conditionRepository;
    }

    public void evaluate(Transaction transaction) {
        TransactionType transactionType = TransactionType.from(transaction.getTransactionType());
        if (transactionType.isAlertExempt()) {
            return;
        }

        List<MonitoringRule> activeRules = ruleRepository.findByIsActive(true);
        for (MonitoringRule rule : activeRules) {
            boolean triggered = "COMPLEX".equals(rule.getRuleType())
                    ? evaluateComplex(transaction, rule)
                    : evaluateSimple(transaction, rule);

            if (triggered) {
                Alert alert = new Alert(rule.getId(), transaction.getId(),
                        transaction.getAccountId(), rule.getSeverity());
                alertRepository.save(alert);
            }
        }
    }

    private boolean evaluateSimple(Transaction tx, MonitoringRule rule) {
        return switch (rule.getRuleType()) {
            case "AMOUNT_THRESHOLD" -> checkAmountThreshold(tx, rule.getThresholdValue());
            case "VELOCITY"         -> checkVelocity(tx, rule.getTimeWindowMinutes(), rule.getMaxCount());
            case "NEW_PAYEE"        -> checkNewPayee(tx);
            case "DAILY_LIMIT"      -> checkDailyLimit(tx, rule.getThresholdValue());
            default -> false;
        };
    }

    private boolean evaluateComplex(Transaction tx, MonitoringRule rule) {
        List<RuleCondition> conditions = conditionRepository.findByRuleId(rule.getId());
        if (conditions.isEmpty()) return false;

        boolean isAnd = "AND".equals(rule.getLogicOperator());
        for (RuleCondition c : conditions) {
            boolean result = evaluateCondition(tx, c);
            if (isAnd && !result) return false;  // AND: one fails → all fail
            if (!isAnd && result) return true;   // OR: one passes → trigger
        }
        return isAnd; // AND: all passed; OR: none passed
    }

    private boolean evaluateCondition(Transaction tx, RuleCondition c) {
        return switch (c.getConditionType()) {
            case "AMOUNT_THRESHOLD" -> checkAmountThreshold(tx, c.getThresholdValue());
            case "VELOCITY"         -> checkVelocity(tx, c.getTimeWindowMinutes(), c.getMaxCount());
            case "NEW_PAYEE"        -> checkNewPayee(tx);
            case "DAILY_LIMIT"      -> checkDailyLimit(tx, c.getThresholdValue());
            case "TIME_OF_DAY"      -> checkTimeOfDay(tx, c.getStartHour(), c.getEndHour());
            default -> false;
        };
    }

    private boolean checkAmountThreshold(Transaction tx, BigDecimal threshold) {
        return tx.getAmount().compareTo(threshold) > 0;
    }

    private boolean checkVelocity(Transaction tx, Integer timeWindowMinutes, Integer maxCount) {
        LocalDateTime since = tx.getCreatedAt().minusMinutes(timeWindowMinutes);
        int count = transactionRepository.countByAccountIdAndCreatedAtAfter(tx.getAccountId(), since);
        return count > maxCount;
    }

    private boolean checkNewPayee(Transaction tx) {
    // 规则3：向从未出现过的收款方转账
    private boolean evaluateNewPayee(Transaction tx, MonitoringRule rule) {
        if (tx.getPayeeId() == null || tx.getPayeeId().isBlank()) {
            return false;
        }
        int previousCount = transactionRepository.countPreviousTransactionsToPayee(
                tx.getAccountId(), tx.getPayeeId(), tx.getId());
        return previousCount == 0;
    }

    private boolean checkDailyLimit(Transaction tx, BigDecimal threshold) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        BigDecimal dailyTotal = transactionRepository.sumAmountByAccountIdAndCreatedAtAfter(
                tx.getAccountId(), startOfDay);
        return dailyTotal.compareTo(threshold) > 0;
    }

    // Triggers when transaction occurs OUTSIDE business hours [startHour, endHour)
    private boolean checkTimeOfDay(Transaction tx, Integer startHour, Integer endHour) {
        int hour = tx.getCreatedAt().getHour();
        return hour < startHour || hour >= endHour;
    }
}
