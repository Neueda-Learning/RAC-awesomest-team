package com.example.monitoring.alert.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("alert")
public class Alert {

    @Id
    private Long id;
    private Long ruleId;
    private Long transactionId;
    private String accountId;
    private String severity;   // HIGH, MEDIUM, LOW
    private AlertStatus status;
    private Integer dedupCount;
    private LocalDateTime lastTriggeredAt;
    private LocalDateTime ackAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime ackDueAt;
    private LocalDateTime resolveDueAt;
    private Boolean slaBreached;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Alert() {}

    public Alert(Long ruleId, Long transactionId, String accountId, String severity) {
        this.ruleId = ruleId;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.severity = severity;
        this.status = AlertStatus.OPEN;
        this.dedupCount = 1;
        this.lastTriggeredAt = LocalDateTime.now();
        this.slaBreached = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }

    public Integer getDedupCount() { return dedupCount; }
    public void setDedupCount(Integer dedupCount) { this.dedupCount = dedupCount; }

    public LocalDateTime getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }

    public LocalDateTime getAckAt() { return ackAt; }
    public void setAckAt(LocalDateTime ackAt) { this.ackAt = ackAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getAckDueAt() { return ackDueAt; }
    public void setAckDueAt(LocalDateTime ackDueAt) { this.ackDueAt = ackDueAt; }

    public LocalDateTime getResolveDueAt() { return resolveDueAt; }
    public void setResolveDueAt(LocalDateTime resolveDueAt) { this.resolveDueAt = resolveDueAt; }

    public Boolean getSlaBreached() { return slaBreached; }
    public void setSlaBreached(Boolean slaBreached) { this.slaBreached = slaBreached; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
