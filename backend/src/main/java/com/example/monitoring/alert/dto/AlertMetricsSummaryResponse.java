package com.example.monitoring.alert.dto;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compact dashboard aggregates, avoiding a full alert-list download for cards and charts.
 */
public class AlertMetricsSummaryResponse {

    private final Instant from;
    private final Instant to;
    private final String severity;
    private final long totalAlerts;
    private final Map<String, Long> statusCounts;
    private final Map<String, Long> severityCounts;

    public AlertMetricsSummaryResponse(Instant from,
                                       Instant to,
                                       String severity,
                                       long totalAlerts,
                                       Map<String, Long> statusCounts,
                                       Map<String, Long> severityCounts) {
        this.from = from;
        this.to = to;
        this.severity = severity;
        this.totalAlerts = totalAlerts;
        this.statusCounts = new LinkedHashMap<>(statusCounts);
        this.severityCounts = new LinkedHashMap<>(severityCounts);
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    public String getSeverity() {
        return severity;
    }

    public long getTotalAlerts() {
        return totalAlerts;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public Map<String, Long> getSeverityCounts() {
        return severityCounts;
    }
}
