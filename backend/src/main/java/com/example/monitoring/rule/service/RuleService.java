package com.example.monitoring.rule.service;

import com.example.monitoring.rule.entity.MonitoringRule;
import com.example.monitoring.rule.repository.MonitoringRuleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RuleService {

    private final MonitoringRuleRepository ruleRepository;

    public RuleService(MonitoringRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<MonitoringRule> getAllRules() {
        return (List<MonitoringRule>) ruleRepository.findAll();
    }

    public Optional<MonitoringRule> getRuleById(Long id) {
        return ruleRepository.findById(id);
    }

    public MonitoringRule createRule(MonitoringRule rule) {
        // Ensure insert payload satisfies NOT NULL timestamp columns in schema.sql
        LocalDateTime now = LocalDateTime.now();
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(now);
        }
        if (rule.getUpdatedAt() == null) {
            rule.setUpdatedAt(now);
        }
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
        return ruleRepository.save(existing);
    }

    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }
}
