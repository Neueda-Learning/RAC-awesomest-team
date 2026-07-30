package com.example.monitoring.alert.dto;

import java.time.Instant;
import java.util.List;

public class AlertTrendResponse {

    private final int days;
    private final Instant from;
    private final Instant to;
    private final String severity;
    private final String timeZone;
    private final List<AlertTrendBucket> buckets;

    public AlertTrendResponse(int days,
                              Instant from,
                              Instant to,
                              String severity,
                              List<AlertTrendBucket> buckets) {
        this.days = days;
        this.from = from;
        this.to = to;
        this.severity = severity;
        this.timeZone = "UTC";
        this.buckets = List.copyOf(buckets);
    }

    public int getDays() {
        return days;
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

    public String getTimeZone() {
        return timeZone;
    }

    public List<AlertTrendBucket> getBuckets() {
        return buckets;
    }
}
