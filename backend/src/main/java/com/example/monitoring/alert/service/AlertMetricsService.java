package com.example.monitoring.alert.service;

import com.example.monitoring.alert.dto.AlertAverageResolutionResponse;
import com.example.monitoring.alert.dto.AlertDashboardMetricsResponse;
import com.example.monitoring.alert.dto.AlertMetricsSummaryResponse;
import com.example.monitoring.alert.dto.AlertTrendBucket;
import com.example.monitoring.alert.dto.AlertTrendResponse;
import com.example.monitoring.alert.entity.AlertStatus;
import com.example.monitoring.alert.repository.AlertMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertMetricsService {

    private static final List<Integer> SUPPORTED_TREND_WINDOWS = List.of(7, 30);
    private static final List<String> SEVERITIES = List.of("HIGH", "MEDIUM", "LOW");

    private final AlertMetricsRepository metricsRepository;
    private final Clock clock;

    @Autowired
    public AlertMetricsService(AlertMetricsRepository metricsRepository) {
        this(metricsRepository, Clock.systemUTC());
    }

    public AlertMetricsService(AlertMetricsRepository metricsRepository, Clock clock) {
        this.metricsRepository = metricsRepository;
        this.clock = clock;
    }

    public AlertAverageResolutionResponse getAverageResolution(Instant from,
                                                               Instant to,
                                                               String severity) {
        validateRange(from, to);
        String normalizedSeverity = normalizeSeverity(severity);
        AlertMetricsRepository.AverageResolutionAggregate result = metricsRepository.averageResolution(
                toUtcDateTime(from),
                toUtcDateTime(to),
                normalizedSeverity
        );

        return new AlertAverageResolutionResponse(
                from,
                to,
                normalizedSeverity,
                result == null ? 0L : result.resolvedAlertCount(),
                result == null ? null : result.averageResolutionSeconds()
        );
    }

    public AlertTrendResponse getRecentTrend(int days, String severity) {
        ReportingWindow window = getReportingWindow(days);
        String normalizedSeverity = normalizeSeverity(severity);

        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (int offset = 0; offset < days; offset++) {
            counts.put(window.startDate().plusDays(offset), 0L);
        }
        for (AlertMetricsRepository.DailyAlertCount row
                : metricsRepository.countCreatedAlertsByDay(
                        window.from(), window.to(), normalizedSeverity)) {
            if (counts.containsKey(row.date())) {
                counts.put(row.date(), row.count());
            }
        }

        List<AlertTrendBucket> buckets = counts.entrySet().stream()
                .map(entry -> new AlertTrendBucket(entry.getKey(), entry.getValue()))
                .toList();

        return new AlertTrendResponse(
                days,
                window.fromInstant(),
                window.toInstant(),
                normalizedSeverity,
                buckets
        );
    }

    public AlertDashboardMetricsResponse getDashboardMetrics(int days, String severity) {
        ReportingWindow window = getReportingWindow(days);
        String normalizedSeverity = normalizeSeverity(severity);

        Map<String, Long> statusCounts = fillStatusCounts(
                metricsRepository.countByStatus(window.from(), window.to(), normalizedSeverity));
        Map<String, Long> severityCounts = fillSeverityCounts(
                metricsRepository.countBySeverity(window.from(), window.to(), normalizedSeverity));
        long totalAlerts = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long totalTransactions = metricsRepository.countTransactions(window.from(), window.to());

        AlertMetricsRepository.DurationAggregate acknowledge =
                metricsRepository.averageAcknowledge(window.from(), window.to(), normalizedSeverity);
        AlertMetricsRepository.AverageResolutionAggregate resolution =
                metricsRepository.averageResolution(window.from(), window.to(), normalizedSeverity);
        AlertMetricsRepository.SlaAggregate sla =
                metricsRepository.getSlaAggregate(window.from(), window.to(), normalizedSeverity);
        AlertMetricsRepository.ResolutionOutcomeAggregate outcomes =
                metricsRepository.getResolutionOutcomes(window.from(), window.to(), normalizedSeverity);

        AlertDashboardMetricsResponse.DashboardKpis kpis =
                new AlertDashboardMetricsResponse.DashboardKpis(
                        totalAlerts,
                        totalTransactions,
                        percentage(totalAlerts, totalTransactions),
                        acknowledge.alertCount(),
                        acknowledge.averageSeconds(),
                        resolution.resolvedAlertCount(),
                        resolution.averageResolutionSeconds(),
                        percentage(sla.breachedAlerts(), sla.totalAlerts()),
                        percentage(outcomes.dismissedAlerts(), outcomes.resolvedAlerts())
                );

        Map<LocalDate, Long> alertCounts = zeroLongSeries(window);
        for (AlertMetricsRepository.DailyAlertCount row
                : metricsRepository.countCreatedAlertsByDay(
                        window.from(), window.to(), normalizedSeverity)) {
            if (alertCounts.containsKey(row.date())) {
                alertCounts.put(row.date(), row.count());
            }
        }

        Map<LocalDate, Long> transactionCounts = zeroLongSeries(window);
        for (AlertMetricsRepository.DailyAlertCount row
                : metricsRepository.countTransactionsByDay(window.from(), window.to())) {
            if (transactionCounts.containsKey(row.date())) {
                transactionCounts.put(row.date(), row.count());
            }
        }

        Map<LocalDate, Double> acknowledgeTimes = new LinkedHashMap<>();
        Map<LocalDate, Double> resolutionTimes = new LinkedHashMap<>();
        for (LocalDate date : alertCounts.keySet()) {
            acknowledgeTimes.put(date, null);
            resolutionTimes.put(date, null);
        }
        metricsRepository.averageAcknowledgeByDay(window.from(), window.to(), normalizedSeverity)
                .forEach(row -> {
                    if (acknowledgeTimes.containsKey(row.date())) {
                        acknowledgeTimes.put(row.date(), row.averageSeconds());
                    }
                });
        metricsRepository.averageResolutionByDay(window.from(), window.to(), normalizedSeverity)
                .forEach(row -> {
                    if (resolutionTimes.containsKey(row.date())) {
                        resolutionTimes.put(row.date(), row.averageSeconds());
                    }
                });

        Map<LocalDate, AlertMetricsRepository.DailySlaCount> slaCounts = new LinkedHashMap<>();
        for (LocalDate date : alertCounts.keySet()) {
            slaCounts.put(date, new AlertMetricsRepository.DailySlaCount(date, 0L, 0L));
        }
        metricsRepository.getDailySlaCounts(window.from(), window.to(), normalizedSeverity)
                .forEach(row -> {
                    if (slaCounts.containsKey(row.date())) {
                        slaCounts.put(row.date(), row);
                    }
                });

        List<AlertTrendBucket> alertTrend = alertCounts.entrySet().stream()
                .map(entry -> new AlertTrendBucket(entry.getKey(), entry.getValue()))
                .toList();
        List<AlertDashboardMetricsResponse.TransactionTrendBucket> transactionTrend =
                transactionCounts.entrySet().stream()
                        .map(entry -> new AlertDashboardMetricsResponse.TransactionTrendBucket(
                                entry.getKey(), entry.getValue()))
                        .toList();
        List<AlertDashboardMetricsResponse.ResponseTimeTrendBucket> responseTimeTrend =
                acknowledgeTimes.keySet().stream()
                        .map(date -> new AlertDashboardMetricsResponse.ResponseTimeTrendBucket(
                                date,
                                acknowledgeTimes.get(date),
                                resolutionTimes.get(date)))
                        .toList();
        List<AlertDashboardMetricsResponse.SlaTrendBucket> slaTrend =
                slaCounts.values().stream()
                        .map(row -> new AlertDashboardMetricsResponse.SlaTrendBucket(
                                row.date(),
                                row.totalAlerts(),
                                row.breachedAlerts(),
                                percentage(row.breachedAlerts(), row.totalAlerts())))
                        .toList();
        List<AlertDashboardMetricsResponse.RuleAlertCount> alertsByRule =
                metricsRepository.countAlertsByRule(window.from(), window.to(), normalizedSeverity)
                        .stream()
                        .map(row -> new AlertDashboardMetricsResponse.RuleAlertCount(
                                row.ruleId(), row.ruleName(), row.count()))
                        .toList();

        return new AlertDashboardMetricsResponse(
                days,
                window.fromInstant(),
                window.toInstant(),
                normalizedSeverity,
                kpis,
                statusCounts,
                severityCounts,
                alertTrend,
                transactionTrend,
                responseTimeTrend,
                slaTrend,
                alertsByRule
        );
    }

    public AlertMetricsSummaryResponse getSummary(Instant from,
                                                  Instant to,
                                                  String severity) {
        validateRange(from, to);
        String normalizedSeverity = normalizeSeverity(severity);
        LocalDateTime utcFrom = toUtcDateTime(from);
        LocalDateTime utcTo = toUtcDateTime(to);

        Map<String, Long> repositoryStatusCounts =
                metricsRepository.countByStatus(utcFrom, utcTo, normalizedSeverity);
        Map<String, Long> repositorySeverityCounts =
                metricsRepository.countBySeverity(utcFrom, utcTo, normalizedSeverity);

        Map<String, Long> statusCounts = fillStatusCounts(repositoryStatusCounts);
        Map<String, Long> severityCounts = fillSeverityCounts(repositorySeverityCounts);

        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        return new AlertMetricsSummaryResponse(
                from,
                to,
                normalizedSeverity,
                total,
                statusCounts,
                severityCounts
        );
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return null;
        }
        String normalized = severity.trim().toUpperCase();
        if (!SEVERITIES.contains(normalized)) {
            throw new IllegalArgumentException("severity must be one of HIGH, MEDIUM, LOW");
        }
        return normalized;
    }

    private LocalDateTime toUtcDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private ReportingWindow getReportingWindow(int days) {
        if (!SUPPORTED_TREND_WINDOWS.contains(days)) {
            throw new IllegalArgumentException("days must be one of 7, 30");
        }
        LocalDate endDate = LocalDate.now(clock);
        LocalDate startDate = endDate.minusDays(days - 1L);
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();
        return new ReportingWindow(startDate, from, to);
    }

    private Map<LocalDate, Long> zeroLongSeries(ReportingWindow window) {
        Map<LocalDate, Long> values = new LinkedHashMap<>();
        LocalDate date = window.startDate();
        while (date.isBefore(window.to().toLocalDate())) {
            values.put(date, 0L);
            date = date.plusDays(1);
        }
        return values;
    }

    private Map<String, Long> fillStatusCounts(Map<String, Long> repositoryCounts) {
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        Arrays.stream(AlertStatus.values())
                .forEach(status -> statusCounts.put(
                        status.name(),
                        repositoryCounts.getOrDefault(status.name(), 0L)));
        return statusCounts;
    }

    private Map<String, Long> fillSeverityCounts(Map<String, Long> repositoryCounts) {
        Map<String, Long> severityCounts = new LinkedHashMap<>();
        SEVERITIES.forEach(value -> severityCounts.put(
                value,
                repositoryCounts.getOrDefault(value, 0L)));
        return severityCounts;
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0.0;
        }
        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private record ReportingWindow(
            LocalDate startDate,
            LocalDateTime from,
            LocalDateTime to) {

        private Instant fromInstant() {
            return from.toInstant(ZoneOffset.UTC);
        }

        private Instant toInstant() {
            return to.toInstant(ZoneOffset.UTC);
        }
    }
}
