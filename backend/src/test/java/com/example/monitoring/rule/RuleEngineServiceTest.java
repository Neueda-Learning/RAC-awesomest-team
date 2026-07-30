package com.example.monitoring.rule;

import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertStatus;
import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.rule.entity.MonitoringRule;
import com.example.monitoring.rule.repository.MonitoringRuleRepository;
import com.example.monitoring.rule.repository.RuleConditionRepository;
import com.example.monitoring.rule.service.RuleEngineService;
import com.example.monitoring.transaction.entity.Transaction;
import com.example.monitoring.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RuleEngineServiceTest {

    @Mock
    private MonitoringRuleRepository ruleRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private RuleConditionRepository conditionRepository;

    @InjectMocks
    private RuleEngineService ruleEngineService;

    @Test
    void evaluate_shouldCreateNewAlertWithSlaDefaults_whenTriggeredAndNoDedupMatch() {
        MonitoringRule rule = buildAmountRule(1L, "HIGH", new BigDecimal("10000"));
        Transaction tx = buildTransaction(10L, "ACC-001", new BigDecimal("15000"), LocalDateTime.of(2026, 7, 30, 10, 0));

        when(ruleRepository.findByIsActive(true)).thenReturn(List.of(rule));
        when(alertRepository.findLatestActiveForDedup(eq(1L), eq("ACC-001"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ruleEngineService.evaluate(tx);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());

        Alert saved = captor.getValue();
        assertEquals(AlertStatus.OPEN, saved.getStatus());
        assertEquals(1, saved.getDedupCount());
        assertEquals(LocalDateTime.of(2026, 7, 30, 10, 0), saved.getLastTriggeredAt());
        assertEquals(LocalDateTime.of(2026, 7, 30, 10, 5), saved.getAckDueAt());
        assertEquals(LocalDateTime.of(2026, 7, 30, 10, 30), saved.getResolveDueAt());
        assertFalse(Boolean.TRUE.equals(saved.getSlaBreached()));
    }

    @Test
    void evaluate_shouldMergeIntoExistingAlert_whenDedupCandidateExists() {
        MonitoringRule rule = buildAmountRule(2L, "MEDIUM", new BigDecimal("10000"));
        Transaction tx = buildTransaction(20L, "ACC-009", new BigDecimal("20000"), LocalDateTime.of(2026, 7, 30, 11, 0));

        Alert existing = new Alert(2L, 19L, "ACC-009", "MEDIUM");
        existing.setId(88L);
        existing.setStatus(AlertStatus.OPEN);
        existing.setDedupCount(2);
        existing.setLastTriggeredAt(LocalDateTime.of(2026, 7, 30, 10, 55));

        when(ruleRepository.findByIsActive(true)).thenReturn(List.of(rule));
        when(alertRepository.findLatestActiveForDedup(eq(2L), eq("ACC-009"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existing));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ruleEngineService.evaluate(tx);

        verify(alertRepository, times(1)).save(existing);
        assertEquals(3, existing.getDedupCount());
        assertEquals(20L, existing.getTransactionId());
        assertEquals(LocalDateTime.of(2026, 7, 30, 11, 0), existing.getLastTriggeredAt());
    }

    private MonitoringRule buildAmountRule(Long id, String severity, BigDecimal threshold) {
        MonitoringRule rule = new MonitoringRule();
        rule.setId(id);
        rule.setRuleName("Amount Rule " + id);
        rule.setRuleType("AMOUNT_THRESHOLD");
        rule.setSeverity(severity);
        rule.setThresholdValue(threshold);
        rule.setActive(true);
        return rule;
    }

    private Transaction buildTransaction(Long id, String accountId, BigDecimal amount, LocalDateTime createdAt) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setAccountId(accountId);
        tx.setPayeeId("PAY-001");
        tx.setAmount(amount);
        tx.setCurrency("USD");
        tx.setTransactionType("TRANSFER_OUT");
        tx.setDescription("test");
        tx.setCreatedAt(createdAt);
        return tx;
    }
}

