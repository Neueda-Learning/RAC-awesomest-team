package com.example.monitoring.alert.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete, compact payload for the alert operations dashboard.
 */
public class AlertDashboardMetricsResponse {

    private final int days;
    private final Instant from;
    private final Instant to;
    private final String severity;
    private final String timeZone;
    private final DashboardKpis kpis;
    private final Map<String, Long> statusCounts;
    private final Map<String, Long> severityCounts;
    private final List<AlertTrendBucket> alertTrend;
    private final List<TransactionTrendBucket> transactionTrend;
    private final List<ResponseTimeTrendBucket> responseTimeTrend;
    private final List<SlaTrendBucket> slaTrend;
    private final List<RuleAlertCount> alertsByRule;

    public AlertDashboardMetricsResponse(int days,
                                         Instant from,
                                         Instant to,
                                         String severity,
                                         DashboardKpis kpis,
                                         Map<String, Long> statusCounts,
                                         Map<String, Long> severityCounts,
                                         List<AlertTrendBucket> alertTrend,
                                         List<TransactionTrendBucket> transactionTrend,
                                         List<ResponseTimeTrendBucket> responseTimeTrend,
                                         List<SlaTrendBucket> slaTrend,
                                         List<RuleAlertCount> alertsByRule) {
        this.days = days;
        this.from = from;
        this.to = to;
        this.severity = severity;
        this.timeZone = "UTC";
        this.kpis = kpis;
        this.statusCounts = new LinkedHashMap<>(statusCounts);
        this.severityCounts = new LinkedHashMap<>(severityCounts);
        this.alertTrend = List.copyOf(alertTrend);
        this.transactionTrend = List.copyOf(transactionTrend);
        this.responseTimeTrend = List.copyOf(responseTimeTrend);
        this.slaTrend = List.copyOf(slaTrend);
        this.alertsByRule = List.copyOf(alertsByRule);
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

    public DashboardKpis getKpis() {
        return kpis;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public Map<String, Long> getSeverityCounts() {
        return severityCounts;
    }

    public List<AlertTrendBucket> getAlertTrend() {
        return alertTrend;
    }

    public List<TransactionTrendBucket> getTransactionTrend() {
        return transactionTrend;
    }

    public List<ResponseTimeTrendBucket> getResponseTimeTrend() {
        return responseTimeTrend;
    }

    public List<SlaTrendBucket> getSlaTrend() {
        return slaTrend;
    }

    public List<RuleAlertCount> getAlertsByRule() {
        return alertsByRule;
    }

    public record DashboardKpis(
            long totalAlerts,
            long totalTransactions,
            double alertTriggerRatePercent,
            long acknowledgedAlertCount,
            Double averageAcknowledgeSeconds,
            long resolvedAlertCount,
            Double averageResolutionSeconds,
            double slaBreachRatePercent,
            double falsePositiveRatePercent) {
    }

    public record TransactionTrendBucket(LocalDate date, long count) {
    }

    public record ResponseTimeTrendBucket(
            LocalDate date,
            Double averageAcknowledgeSeconds,
            Double averageResolutionSeconds) {
    }

    public record SlaTrendBucket(
            LocalDate date,
            long totalAlerts,
            long breachedAlerts,
            double breachRatePercent) {
    }

    public record RuleAlertCount(Long ruleId, String ruleName, long count) {
    }
}
