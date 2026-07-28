package com.example.monitoring.rule.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;

@Table("rule_condition")
public class RuleCondition {

    @Id
    private Long id;
    private Long ruleId;
    private String conditionType; // AMOUNT_THRESHOLD, VELOCITY, NEW_PAYEE, DAILY_LIMIT, TIME_OF_DAY
    private BigDecimal thresholdValue;
    private Integer timeWindowMinutes;
    private Integer maxCount;
    private Integer startHour;  // for TIME_OF_DAY
    private Integer endHour;    // for TIME_OF_DAY

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getConditionType() { return conditionType; }
    public void setConditionType(String conditionType) { this.conditionType = conditionType; }

    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }

    public Integer getTimeWindowMinutes() { return timeWindowMinutes; }
    public void setTimeWindowMinutes(Integer timeWindowMinutes) { this.timeWindowMinutes = timeWindowMinutes; }

    public Integer getMaxCount() { return maxCount; }
    public void setMaxCount(Integer maxCount) { this.maxCount = maxCount; }

    public Integer getStartHour() { return startHour; }
    public void setStartHour(Integer startHour) { this.startHour = startHour; }

    public Integer getEndHour() { return endHour; }
    public void setEndHour(Integer endHour) { this.endHour = endHour; }
}
