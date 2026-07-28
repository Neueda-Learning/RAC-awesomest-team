package com.example.monitoring.rule.repository;

import com.example.monitoring.rule.entity.RuleCondition;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface RuleConditionRepository extends CrudRepository<RuleCondition, Long> {
    List<RuleCondition> findByRuleId(Long ruleId);
    void deleteByRuleId(Long ruleId);
}
