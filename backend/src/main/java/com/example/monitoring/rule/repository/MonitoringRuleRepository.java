package com.example.monitoring.rule.repository;

import com.example.monitoring.rule.entity.MonitoringRule;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MonitoringRuleRepository extends CrudRepository<MonitoringRule, Long> {

    List<MonitoringRule> findByIsActive(boolean isActive);
}
