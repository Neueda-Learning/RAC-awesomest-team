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
    private String status;     // OPEN, ACKNOWLEDGED, INVESTIGATING, CLOSED, DISMISSED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Alert() {}

    public Alert(Long ruleId, Long transactionId, String accountId, String severity) {
        this.ruleId = ruleId;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.severity = severity;
        this.status = "OPEN";
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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
