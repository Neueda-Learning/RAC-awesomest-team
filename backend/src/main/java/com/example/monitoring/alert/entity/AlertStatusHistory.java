package com.example.monitoring.alert.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("alert_status_history")
public class AlertStatusHistory {

    @Id
    private Long id;
    private Long alertId;
    private AlertStatus oldStatus;
    private AlertStatus newStatus;
    private String notes;
    private LocalDateTime changedAt;

    public AlertStatusHistory() {}

    public AlertStatusHistory(Long alertId, AlertStatus oldStatus, AlertStatus newStatus, String notes) {
        this.alertId = alertId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.notes = notes;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }

    public AlertStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(AlertStatus oldStatus) { this.oldStatus = oldStatus; }

    public AlertStatus getNewStatus() { return newStatus; }
    public void setNewStatus(AlertStatus newStatus) { this.newStatus = newStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
