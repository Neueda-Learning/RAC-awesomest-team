package com.example.monitoring.alert.dto;

import com.example.monitoring.alert.entity.AlertStatus;

import java.time.LocalDateTime;

/**
 * Lightweight alert row model for list pages.
 */
public class AlertListItem {

    private Long id;
    private Long ruleId;
    private String accountId;
    private String severity;
    private AlertStatus status;
    private LocalDateTime createdAt;

    public AlertListItem() {
    }

    public AlertListItem(Long id, Long ruleId, String accountId, String severity, AlertStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.ruleId = ruleId;
        this.accountId = accountId;
        this.severity = severity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

