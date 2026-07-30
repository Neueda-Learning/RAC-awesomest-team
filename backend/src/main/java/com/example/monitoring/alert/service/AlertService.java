package com.example.monitoring.alert.service;

import com.example.monitoring.alert.dto.AlertBulkAction;
import com.example.monitoring.alert.dto.AlertQueryRequest;
import com.example.monitoring.alert.dto.AlertQueryResponse;
import com.example.monitoring.alert.dto.AlertListItem;
import com.example.monitoring.alert.dto.BulkAlertStatusRequest;
import com.example.monitoring.alert.dto.BulkAlertStatusResponse;
import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertStatus;
import com.example.monitoring.alert.entity.AlertStatusHistory;
import com.example.monitoring.alert.repository.AlertQueryRepository;
import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.alert.repository.AlertStatusHistoryRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_BULK_ALERTS = 100;
    private static final int MAX_EXPORT_ROWS = 5_000;

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

    /**
     * Applies one lifecycle action to multiple alerts. An invalid transition for
     * one alert does not prevent the remaining alerts from being processed.
     */
    public BulkAlertStatusResponse bulkChangeStatus(BulkAlertStatusRequest request) {
        validateBulkRequest(request);

        String notes = normalizeNotes(request.getNotes());
        List<BulkAlertStatusResponse.ItemResult> results = new ArrayList<>();
        int successCount = 0;

        for (Long id : request.getIds()) {
            try {
                Alert updated = applyBulkAction(id, request.getAction(), notes);
                results.add(new BulkAlertStatusResponse.ItemResult(
                        id, true, updated.getStatus(), null));
                successCount++;
            } catch (IllegalArgumentException | IllegalStateException exception) {
                results.add(new BulkAlertStatusResponse.ItemResult(
                        id, false, null, exception.getMessage()));
            }
        }

        return new BulkAlertStatusResponse(
                request.getIds().size(),
                successCount,
                request.getIds().size() - successCount,
                results
        );
    }

    /**
     * Exports alerts using the same filters and ordering as the query endpoint.
     */
    public byte[] exportAlertsCsv(AlertQueryRequest request) {
        AlertQueryRequest normalized = validateAndNormalizeQueryRequest(request);
        List<Alert> alerts = alertQueryRepository.findForExport(normalized, MAX_EXPORT_ROWS + 1);
        if (alerts.size() > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException(
                    "Export exceeds the maximum of " + MAX_EXPORT_ROWS + " alerts; refine the filters");
        }

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("id,ruleId,transactionId,accountId,severity,status,dedupCount,lastTriggeredAt,")
                .append("slaBreached,ackAt,resolvedAt,ackDueAt,resolveDueAt,createdAt,updatedAt\r\n");
        for (Alert alert : alerts) {
            appendCsvRow(csv,
                    alert.getId(),
                    alert.getRuleId(),
                    alert.getTransactionId(),
                    alert.getAccountId(),
                    alert.getSeverity(),
                    alert.getStatus(),
                    alert.getDedupCount(),
                    alert.getLastTriggeredAt(),
                    alert.getSlaBreached(),
                    alert.getAckAt(),
                    alert.getResolvedAt(),
                    alert.getAckDueAt(),
                    alert.getResolveDueAt(),
                    alert.getCreatedAt(),
                    alert.getUpdatedAt());
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
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

        if (normalized.getStatusGroup() != null) {
            String statusGroup = normalized.getStatusGroup().trim().toUpperCase();
            if (statusGroup.isEmpty()) {
                normalized.setStatusGroup(null);
            } else if (!"ACTIVE".equals(statusGroup) && !"RESOLVED".equals(statusGroup)) {
                throw new IllegalArgumentException("statusGroup must be one of ACTIVE, RESOLVED");
            } else {
                normalized.setStatusGroup(statusGroup);
            }
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

    private void validateBulkRequest(BulkAlertStatusRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("ids must not be empty");
        }
        if (request.getIds().size() > MAX_BULK_ALERTS) {
            throw new IllegalArgumentException("ids must contain at most " + MAX_BULK_ALERTS + " alerts");
        }
        if (request.getAction() == null) {
            throw new IllegalArgumentException("action is required");
        }

        Set<Long> uniqueIds = new HashSet<>();
        for (Long id : request.getIds()) {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("ids must contain only positive values");
            }
            if (!uniqueIds.add(id)) {
                throw new IllegalArgumentException("ids must not contain duplicates");
            }
        }
        if (request.getNotes() != null && request.getNotes().length() > 1000) {
            throw new IllegalArgumentException("notes must not exceed 1000 characters");
        }
    }

    private Alert applyBulkAction(Long id, AlertBulkAction action, String notes) {
        return switch (action) {
            case ACKNOWLEDGE -> acknowledge(id, notes);
            case INVESTIGATE -> startInvestigating(id, notes);
            case CLOSE -> close(id, notes);
            case DISMISS -> dismiss(id, notes);
        };
    }

    private String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String normalized = notes.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void appendCsvRow(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escapeCsv(values[index]));
        }
        csv.append("\r\n");
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\r') >= 0 || text.indexOf('\n') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
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
