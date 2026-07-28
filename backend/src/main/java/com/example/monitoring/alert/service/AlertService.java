package com.example.monitoring.alert.service;

import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertStatusHistory;
import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.alert.repository.AlertStatusHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertStatusHistoryRepository historyRepository;

    public AlertService(AlertRepository alertRepository,
                        AlertStatusHistoryRepository historyRepository) {
        this.alertRepository = alertRepository;
        this.historyRepository = historyRepository;
    }

    public List<Alert> getAllAlerts() {
        return (List<Alert>) alertRepository.findAll();
    }

    public List<Alert> getAlertsByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    public List<AlertStatusHistory> getAlertHistory(Long alertId) {
        return historyRepository.findByAlertId(alertId);
    }

    /**
     * 通用的状态流转方法，同时记录历史
     */
    private Alert changeStatus(Long alertId, String newStatus, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        String oldStatus = alert.getStatus();
        alert.setStatus(newStatus);
        alert.setUpdatedAt(LocalDateTime.now());
        alertRepository.save(alert);

        historyRepository.save(new AlertStatusHistory(alertId, oldStatus, newStatus, notes));
        return alert;
    }

    // OPEN → ACKNOWLEDGED
    public Alert acknowledge(Long alertId, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        if (!"OPEN".equals(alert.getStatus())) {
            throw new IllegalStateException("Can only acknowledge OPEN alerts. Current status: " + alert.getStatus());
        }
        return changeStatus(alertId, "ACKNOWLEDGED", notes);
    }

    // ACKNOWLEDGED → INVESTIGATING
    public Alert startInvestigating(Long alertId, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        if (!"ACKNOWLEDGED".equals(alert.getStatus())) {
            throw new IllegalStateException("Can only investigate ACKNOWLEDGED alerts. Current status: " + alert.getStatus());
        }
        return changeStatus(alertId, "INVESTIGATING", notes);
    }

    // ACKNOWLEDGED or INVESTIGATING → CLOSED
    public Alert close(Long alertId, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        if (!"ACKNOWLEDGED".equals(alert.getStatus()) && !"INVESTIGATING".equals(alert.getStatus())) {
            throw new IllegalStateException("Can only close ACKNOWLEDGED or INVESTIGATING alerts. Current status: " + alert.getStatus());
        }
        return changeStatus(alertId, "CLOSED", notes);
    }

    // ACKNOWLEDGED or INVESTIGATING → DISMISSED
    public Alert dismiss(Long alertId, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        if (!"ACKNOWLEDGED".equals(alert.getStatus()) && !"INVESTIGATING".equals(alert.getStatus())) {
            throw new IllegalStateException("Can only dismiss ACKNOWLEDGED or INVESTIGATING alerts. Current status: " + alert.getStatus());
        }
        return changeStatus(alertId, "DISMISSED", notes);
    }
}
