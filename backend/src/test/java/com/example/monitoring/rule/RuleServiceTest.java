package com.example.monitoring.rule;

import com.example.monitoring.rule.entity.MonitoringRule;
import com.example.monitoring.rule.repository.MonitoringRuleRepository;
import com.example.monitoring.rule.service.RuleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RuleServiceTest {

	@Mock
	private MonitoringRuleRepository ruleRepository;

	@InjectMocks
	private RuleService ruleService;

	@Test
	void createRule_shouldFillTimestampsWhenMissing() {
		MonitoringRule rule = new MonitoringRule();
		rule.setRuleName("Threshold Rule");

		when(ruleRepository.save(any(MonitoringRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		MonitoringRule saved = ruleService.createRule(rule);

		assertNotNull(saved.getCreatedAt());
		assertNotNull(saved.getUpdatedAt());
	}

	@Test
	void createRule_shouldKeepProvidedTimestamps() {
		MonitoringRule rule = new MonitoringRule();
		LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
		LocalDateTime updatedAt = LocalDateTime.now().minusHours(1);
		rule.setCreatedAt(createdAt);
		rule.setUpdatedAt(updatedAt);

		when(ruleRepository.save(any(MonitoringRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		MonitoringRule saved = ruleService.createRule(rule);

		assertSame(createdAt, saved.getCreatedAt());
		assertSame(updatedAt, saved.getUpdatedAt());
	}

	@Test
	void updateRule_shouldUpdateExistingRule() {
		MonitoringRule existing = new MonitoringRule();
		existing.setId(9L);
		existing.setRuleName("Old");

		MonitoringRule updated = new MonitoringRule();
		updated.setRuleName("New Name");
		updated.setRuleType("AMOUNT_THRESHOLD");
		updated.setSeverity("HIGH");
		updated.setActive(true);
		updated.setThresholdValue(new BigDecimal("999.99"));
		updated.setTimeWindowMinutes(15);
		updated.setMaxCount(3);

		when(ruleRepository.findById(9L)).thenReturn(Optional.of(existing));
		when(ruleRepository.save(any(MonitoringRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		MonitoringRule result = ruleService.updateRule(9L, updated);

		assertEquals("New Name", result.getRuleName());
		assertEquals("AMOUNT_THRESHOLD", result.getRuleType());
		assertEquals("HIGH", result.getSeverity());
		assertEquals(new BigDecimal("999.99"), result.getThresholdValue());
		assertEquals(15, result.getTimeWindowMinutes());
		assertEquals(3, result.getMaxCount());
		verify(ruleRepository).save(existing);
	}

	@Test
	void updateRule_shouldThrowWhenRuleNotFound() {
		when(ruleRepository.findById(404L)).thenReturn(Optional.empty());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> ruleService.updateRule(404L, new MonitoringRule()));
		assertEquals("Rule not found: 404", ex.getMessage());
	}
}
