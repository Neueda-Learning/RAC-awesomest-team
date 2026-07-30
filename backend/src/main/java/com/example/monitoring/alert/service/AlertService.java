package com.example.monitoring.alert.service;

import com.example.monitoring.alert.dto.AlertQueryRequest;
import com.example.monitoring.alert.dto.AlertQueryResponse;
import com.example.monitoring.alert.dto.AlertListItem;
import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertStatus;
import com.example.monitoring.alert.entity.AlertStatusHistory;
import com.example.monitoring.alert.repository.AlertQueryRepository;
import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.alert.repository.AlertStatusHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AlertRepository alertRepository;
    private final AlertStatusHistoryRepository historyRepository;
    private final AlertQueryRepository alertQueryRepository;

    public AlertService(AlertRepository alertRepository,
                        AlertStatusHistoryRepository historyRepository,
                        AlertQueryRepository alertQueryRepository) {
        this.alertRepository = alertRepository;
        this.historyRepository = historyRepository;
        this.alertQueryRepository = alertQueryRepository;
    }

    /**
     * Executes server-side filtering, sorting, and pagination for alerts.
     *
     * @param request optional query fields from request params
     * @return paged alert response
     */
    public AlertQueryResponse queryAlerts(AlertQueryRequest request) {
        AlertQueryRequest validated = validateAndNormalizeQueryRequest(request);
        AlertQueryRepository.AlertQueryResult result = alertQueryRepository.query(validated);
        int totalPages = (int) Math.ceil((double) result.getTotalElements() / validated.getSize());

        List<AlertListItem> rows = result.getContent().stream()
                .map(this::toListItem)
                .collect(Collectors.toList());

        return new AlertQueryResponse(
                rows,
                result.getTotalElements(),
                totalPages,
                validated.getPage(),
                validated.getSize()
        );
    }

    /**
     * Returns SLA-breached alerts using the same query and paging contract.
     */
    public AlertQueryResponse querySlaBreachedAlerts(AlertQueryRequest request) {
        AlertQueryRequest normalized = request == null ? new AlertQueryRequest() : request;
        normalized.setSlaBreached(true);
        return queryAlerts(normalized);
    }

    public List<Alert> getAllAlerts() {
        return (List<Alert>) alertRepository.findAll();
    }

    public List<Alert> getAlertsByStatus(AlertStatus status) {
        return alertRepository.findByStatus(status);
    }

    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    /**
     * Returns full alert detail payload used by row-expansion pages.
     *
     * @param id alert id
     * @return optional alert detail
     */
    public Optional<Alert> getAlertDetailById(Long id) {
        return alertRepository.findById(id);
    }

    public List<AlertStatusHistory> getAlertHistory(Long alertId) {
        return historyRepository.findByAlertId(alertId);
    }

    /**
     * Applies defaults and validates advanced query parameters.
     *
     * @param request incoming query request
     * @return normalized request instance
     * @throws IllegalArgumentException when request values are invalid
     */
    private AlertQueryRequest validateAndNormalizeQueryRequest(AlertQueryRequest request) {
        AlertQueryRequest normalized = request == null ? new AlertQueryRequest() : request;

        if (normalized.getPage() == null) {
            normalized.setPage(0);
        }
        if (normalized.getPage() < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }

        if (normalized.getSize() == null) {
            normalized.setSize(20);
        }
        if (normalized.getSize() <= 0 || normalized.getSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        if (normalized.getRuleId() != null && normalized.getRuleId() <= 0) {
            throw new IllegalArgumentException("ruleId must be greater than 0");
        }

        if (normalized.getFrom() != null && normalized.getTo() != null
                && normalized.getFrom().isAfter(normalized.getTo())) {
            throw new IllegalArgumentException("from must not be after to");
        }

        if (normalized.getSeverity() != null) {
            String severity = normalized.getSeverity().trim().toUpperCase();
            if (!"HIGH".equals(severity) && !"MEDIUM".equals(severity) && !"LOW".equals(severity)) {
                throw new IllegalArgumentException("severity must be one of HIGH, MEDIUM, LOW");
            }
            normalized.setSeverity(severity);
        }

        if (normalized.getAccountId() != null) {
            String accountId = normalized.getAccountId().trim();
            normalized.setAccountId(accountId.isEmpty() ? null : accountId);
        }

        if (normalized.getSortBy() == null || normalized.getSortBy().trim().isEmpty()) {
            normalized.setSortBy("createdAt");
        }
        if (normalized.getSortDir() == null || normalized.getSortDir().trim().isEmpty()) {
            normalized.setSortDir("desc");
        }

        return normalized;
    }

    private AlertListItem toListItem(Alert alert) {
        return new AlertListItem(
                alert.getId(),
                alert.getRuleId(),
                alert.getAccountId(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getCreatedAt(),
                alert.getDedupCount() == null || alert.getDedupCount() < 1 ? 1 : alert.getDedupCount(),
                alert.getLastTriggeredAt(),
                Boolean.TRUE.equals(alert.getSlaBreached())
        );
    }

    /**
     * 通用的状态流转方法，同时记录历史
     */
    private Alert changeStatus(Long alertId, AlertStatus newStatus, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        AlertStatus oldStatus = alert.getStatus();
        LocalDateTime now = LocalDateTime.now();

        if (newStatus == AlertStatus.ACKNOWLEDGED && alert.getAckAt() == null) {
            alert.setAckAt(now);
        }
        if ((newStatus == AlertStatus.CLOSED || newStatus == AlertStatus.DISMISSED) && alert.getResolvedAt() == null) {
            alert.setResolvedAt(now);
        }

        alert.setStatus(newStatus);
        alert.setUpdatedAt(now);
        alert.setSlaBreached(AlertSlaPolicy.isSlaBreached(alert));
        alertRepository.save(alert);

        historyRepository.save(new AlertStatusHistory(alertId, oldStatus, newStatus, notes));
        return alert;
    }

    // OPEN → ACKNOWLEDGED
    public Alert acknowledge(Long alertId, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new IllegalStateException("Can only acknowledge OPEN alerts. Current status: " + alert.getStatus().name());
        }
        return changeStatus(alertId, AlertStatus.ACKNOWLEDGED, notes);
    }

    // ACKNOWLEDGED → INVESTIGATING
    public Alert startInvestigating(Long alertId, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        if (alert.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw new IllegalStateException("Can only investigate ACKNOWLEDGED alerts. Current status: " + alert.getStatus().name());
        }
        return changeStatus(alertId, AlertStatus.INVESTIGATING, notes);
    }

    // INVESTIGATING → CLOSED
    public Alert close(Long alertId, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        if (alert.getStatus() != AlertStatus.INVESTIGATING) {
            throw new IllegalStateException("Can only close INVESTIGATING alerts. Current status: " + alert.getStatus().name());
        }
        return changeStatus(alertId, AlertStatus.CLOSED, notes);
    }

    // ACKNOWLEDGED or INVESTIGATING → DISMISSED
    public Alert dismiss(Long alertId, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        if (alert.getStatus() != AlertStatus.ACKNOWLEDGED && alert.getStatus() != AlertStatus.INVESTIGATING) {
            throw new IllegalStateException("Can only dismiss ACKNOWLEDGED or INVESTIGATING alerts. Current status: " + alert.getStatus().name());
        }
        return changeStatus(alertId, AlertStatus.DISMISSED, notes);
    }
}
