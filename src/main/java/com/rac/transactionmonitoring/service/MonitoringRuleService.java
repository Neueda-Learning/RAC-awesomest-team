package com.rac.transactionmonitoring.service;

import com.rac.transactionmonitoring.model.MonitoringRule;
import com.rac.transactionmonitoring.repository.MonitoringRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MonitoringRuleService {

    private final MonitoringRuleRepository ruleRepository;

    public MonitoringRuleService(MonitoringRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<MonitoringRule> getAllRules() {
        return (List<MonitoringRule>) ruleRepository.findAll();
    }

    public Optional<MonitoringRule> getRuleById(Long id) {
        return ruleRepository.findById(id);
    }

    public MonitoringRule createRule(MonitoringRule rule) {
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

