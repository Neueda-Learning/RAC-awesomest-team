package com.rac.transactionmonitoring.repository;

import com.rac.transactionmonitoring.model.MonitoringRule;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MonitoringRuleRepository extends CrudRepository<MonitoringRule, Long> {

    List<MonitoringRule> findByIsActive(boolean isActive);
}

