package com.example.monitoring.rule.service;

import com.example.monitoring.rule.entity.MonitoringRule;
import com.example.monitoring.rule.entity.RuleCondition;
import com.example.monitoring.rule.repository.MonitoringRuleRepository;
import com.example.monitoring.rule.repository.RuleConditionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RuleService {

    private final MonitoringRuleRepository ruleRepository;
    private final RuleConditionRepository conditionRepository;

    public RuleService(MonitoringRuleRepository ruleRepository,
                       RuleConditionRepository conditionRepository) {
        this.ruleRepository = ruleRepository;
        this.conditionRepository = conditionRepository;
    }

    public List<MonitoringRule> getAllRules() {
        return (List<MonitoringRule>) ruleRepository.findAll();
    }

    public Optional<MonitoringRule> getRuleById(Long id) {
        return ruleRepository.findById(id);
    }

    public MonitoringRule createRule(MonitoringRule rule) {
        LocalDateTime now = LocalDateTime.now();
        if (rule.getCreatedAt() == null) rule.setCreatedAt(now);
        if (rule.getUpdatedAt() == null) rule.setUpdatedAt(now);
        return ruleRepository.save(rule);
    }

    public MonitoringRule updateRule(Long id, MonitoringRule updated) {
        MonitoringRule existing = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));
        existing.setRuleName(updated.getRuleName());
        existing.setRuleType(updated.getRuleType());
        existing.setSeverity(updated.getSeverity());
        existing.setActive(updated.isActive());
        existing.setThresholdValue(updated.getThresholdValue());
        existing.setTimeWindowMinutes(updated.getTimeWindowMinutes());
        existing.setMaxCount(updated.getMaxCount());
        existing.setLogicOperator(updated.getLogicOperator());
        return ruleRepository.save(existing);
    }

    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }

    // ── Condition management ─────────────────────────────────────────────────

    public List<RuleCondition> getConditions(Long ruleId) {
        return conditionRepository.findByRuleId(ruleId);
    }

    public RuleCondition addCondition(Long ruleId, RuleCondition condition) {
        condition.setRuleId(ruleId);
        return conditionRepository.save(condition);
    }

    public void deleteCondition(Long conditionId) {
        conditionRepository.deleteById(conditionId);
    }

    public void deleteAllConditions(Long ruleId) {
        conditionRepository.deleteByRuleId(ruleId);
    }
}
