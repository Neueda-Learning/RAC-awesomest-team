package com.example.monitoring.rule.service;

import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.rule.entity.MonitoringRule;
import com.example.monitoring.rule.repository.MonitoringRuleRepository;
import com.example.monitoring.transaction.entity.Transaction;
import com.example.monitoring.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 规则引擎：每次提交交易后，自动对所有启用规则进行评估，触发则生成 Alert
 */
@Service
public class RuleEngineService {

    private final MonitoringRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;

    public RuleEngineService(MonitoringRuleRepository ruleRepository,
                             TransactionRepository transactionRepository,
                             AlertRepository alertRepository) {
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * 对一笔交易评估所有启用的规则
     */
    public void evaluate(Transaction transaction) {
        List<MonitoringRule> activeRules = ruleRepository.findByIsActive(true);
        for (MonitoringRule rule : activeRules) {
            boolean triggered = switch (rule.getRuleType()) {
                case "AMOUNT_THRESHOLD" -> evaluateAmountThreshold(transaction, rule);
                case "VELOCITY" -> evaluateVelocity(transaction, rule);
                case "NEW_PAYEE" -> evaluateNewPayee(transaction, rule);
                case "DAILY_LIMIT" -> evaluateDailyLimit(transaction, rule);
                default -> false;
            };

            if (triggered) {
                Alert alert = new Alert(rule.getId(), transaction.getId(),
                        transaction.getAccountId(), rule.getSeverity());
                alertRepository.save(alert);
            }
        }
    }

    // 规则1：单笔金额超过阈值
    private boolean evaluateAmountThreshold(Transaction tx, MonitoringRule rule) {
        return tx.getAmount().compareTo(rule.getThresholdValue()) > 0;
    }

    // 规则2：时间窗口内交易次数超过上限
    private boolean evaluateVelocity(Transaction tx, MonitoringRule rule) {
        LocalDateTime since = tx.getCreatedAt().minusMinutes(rule.getTimeWindowMinutes());
        int count = transactionRepository.countByAccountIdAndCreatedAtAfter(tx.getAccountId(), since);
        return count > rule.getMaxCount();
    }

    // 规则3：向从未出现过的收款方转账
    private boolean evaluateNewPayee(Transaction tx, MonitoringRule rule) {
        int previousCount = transactionRepository.countPreviousTransactionsToPayee(
                tx.getAccountId(), tx.getPayeeId(), tx.getId());
        return previousCount == 0;
    }

    // 规则4：当日累计金额超过每日限额
    private boolean evaluateDailyLimit(Transaction tx, MonitoringRule rule) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        BigDecimal dailyTotal = transactionRepository.sumAmountByAccountIdAndCreatedAtAfter(
                tx.getAccountId(), startOfDay);
        return dailyTotal.compareTo(rule.getThresholdValue()) > 0;
    }
}
