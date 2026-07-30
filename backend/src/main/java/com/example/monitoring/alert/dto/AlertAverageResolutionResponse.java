package com.example.monitoring.alert.dto;

import java.time.Instant;

/**
 * Average time between alert creation and resolution for closed or dismissed alerts.
 */
public class AlertAverageResolutionResponse {

    private final Instant from;
    private final Instant to;
    private final String severity;
    private final long resolvedAlertCount;
    private final Double averageResolutionSeconds;

    public AlertAverageResolutionResponse(Instant from,
                                          Instant to,
                                          String severity,
                                          long resolvedAlertCount,
                                          Double averageResolutionSeconds) {
        this.from = from;
        this.to = to;
        this.severity = severity;
        this.resolvedAlertCount = resolvedAlertCount;
        this.averageResolutionSeconds = averageResolutionSeconds;
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

    public long getResolvedAlertCount() {
        return resolvedAlertCount;
    }

    public Double getAverageResolutionSeconds() {
        return averageResolutionSeconds;
    }
}
