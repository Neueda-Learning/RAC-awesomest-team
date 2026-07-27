package com.rac.transactionmonitoring.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("monitoring_rule")
public class MonitoringRule {

    @Id
    private Long id;
    private String ruleName;
    private String ruleType;       // AMOUNT_THRESHOLD, VELOCITY, NEW_PAYEE, DAILY_LIMIT
    private String severity;       // HIGH, MEDIUM, LOW
    private boolean isActive;
    private BigDecimal thresholdValue;
    private Integer timeWindowMinutes;
    private Integer maxCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MonitoringRule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }

    public Integer getTimeWindowMinutes() { return timeWindowMinutes; }
    public void setTimeWindowMinutes(Integer timeWindowMinutes) { this.timeWindowMinutes = timeWindowMinutes; }

    public Integer getMaxCount() { return maxCount; }
    public void setMaxCount(Integer maxCount) { this.maxCount = maxCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

