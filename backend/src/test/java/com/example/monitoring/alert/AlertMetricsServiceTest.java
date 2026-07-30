package com.example.monitoring.alert;

import com.example.monitoring.alert.dto.AlertAverageResolutionResponse;
import com.example.monitoring.alert.dto.AlertDashboardMetricsResponse;
import com.example.monitoring.alert.dto.AlertMetricsSummaryResponse;
import com.example.monitoring.alert.dto.AlertTrendResponse;
import com.example.monitoring.alert.repository.AlertMetricsRepository;
import com.example.monitoring.alert.service.AlertMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertMetricsServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T15:30:00Z");

    @Mock
    private AlertMetricsRepository metricsRepository;

    private AlertMetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new AlertMetricsService(
                metricsRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void averageResolution_shouldUseUtcRangeAndNormalizeSeverity() {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-01T00:00:00Z");
        when(metricsRepository.averageResolution(
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                "HIGH"
        )).thenReturn(new AlertMetricsRepository.AverageResolutionAggregate(4L, 92.5));

        AlertAverageResolutionResponse response =
                metricsService.getAverageResolution(from, to, " high ");

        assertEquals(4L, response.getResolvedAlertCount());
        assertEquals(92.5, response.getAverageResolutionSeconds());
        assertEquals("HIGH", response.getSeverity());
        assertEquals(from, response.getFrom());
        assertEquals(to, response.getTo());
    }

    @Test
    void averageResolution_shouldReturnNullAverageWhenNoAlertsResolved() {
        when(metricsRepository.averageResolution(null, null, null))
                .thenReturn(new AlertMetricsRepository.AverageResolutionAggregate(0L, null));

        AlertAverageResolutionResponse response =
                metricsService.getAverageResolution(null, null, null);

        assertEquals(0L, response.getResolvedAlertCount());
        assertNull(response.getAverageResolutionSeconds());
    }

    @Test
    void averageResolution_shouldRejectEmptyOrReversedRange() {
        Instant instant = Instant.parse("2026-07-30T00:00:00Z");

        IllegalArgumentException equal = assertThrows(IllegalArgumentException.class,
                () -> metricsService.getAverageResolution(instant, instant, null));
        assertEquals("from must be before to", equal.getMessage());

        IllegalArgumentException reversed = assertThrows(IllegalArgumentException.class,
                () -> metricsService.getAverageResolution(instant.plusSeconds(1), instant, null));
        assertEquals("from must be before to", reversed.getMessage());
    }

    @Test
    void recentTrend_shouldFillAllUtcDaysIncludingZeroCounts() {
        LocalDateTime from = LocalDateTime.of(2026, 7, 24, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 31, 0, 0);
        when(metricsRepository.countCreatedAlertsByDay(from, to, null)).thenReturn(List.of(
                new AlertMetricsRepository.DailyAlertCount(LocalDate.of(2026, 7, 24), 2L),
                new AlertMetricsRepository.DailyAlertCount(LocalDate.of(2026, 7, 27), 5L),
                new AlertMetricsRepository.DailyAlertCount(LocalDate.of(2026, 7, 30), 1L)
        ));

        AlertTrendResponse response = metricsService.getRecentTrend(7, null);

        assertEquals("UTC", response.getTimeZone());
        assertEquals(7, response.getBuckets().size());
        assertEquals(LocalDate.of(2026, 7, 24), response.getBuckets().get(0).getDate());
        assertEquals(2L, response.getBuckets().get(0).getCount());
        assertEquals(0L, response.getBuckets().get(1).getCount());
        assertEquals(5L, response.getBuckets().get(3).getCount());
        assertEquals(1L, response.getBuckets().get(6).getCount());
        assertEquals(Instant.parse("2026-07-24T00:00:00Z"), response.getFrom());
        assertEquals(Instant.parse("2026-07-31T00:00:00Z"), response.getTo());
    }

    @Test
    void recentTrend_shouldRejectUnsupportedWindow() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> metricsService.getRecentTrend(14, null));

        assertEquals("days must be one of 7, 30", exception.getMessage());
    }

    @Test
    void summary_shouldFillMissingCategoriesWithZeros() {
        Instant from = Instant.parse("2026-07-24T00:00:00Z");
        Instant to = Instant.parse("2026-07-31T00:00:00Z");
        LocalDateTime localFrom = LocalDateTime.of(2026, 7, 24, 0, 0);
        LocalDateTime localTo = LocalDateTime.of(2026, 7, 31, 0, 0);
        when(metricsRepository.countByStatus(localFrom, localTo, null))
                .thenReturn(Map.of("OPEN", 3L, "CLOSED", 2L));
        when(metricsRepository.countBySeverity(localFrom, localTo, null))
                .thenReturn(Map.of("HIGH", 4L, "LOW", 1L));

        AlertMetricsSummaryResponse response = metricsService.getSummary(from, to, null);

        assertEquals(5L, response.getTotalAlerts());
        assertEquals(3L, response.getStatusCounts().get("OPEN"));
        assertEquals(0L, response.getStatusCounts().get("ACKNOWLEDGED"));
        assertEquals(2L, response.getStatusCounts().get("CLOSED"));
        assertEquals(4L, response.getSeverityCounts().get("HIGH"));
        assertEquals(0L, response.getSeverityCounts().get("MEDIUM"));
    }

    @Test
    void metrics_shouldRejectInvalidSeverity() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> metricsService.getRecentTrend(7, "critical"));

        assertEquals("severity must be one of HIGH, MEDIUM, LOW", exception.getMessage());
    }

    @Test
    void dashboardMetrics_shouldBuildKpisAndZeroFilledChartSeries() {
        LocalDateTime from = LocalDateTime.of(2026, 7, 24, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 31, 0, 0);
        when(metricsRepository.countByStatus(from, to, null))
                .thenReturn(Map.of("OPEN", 2L, "CLOSED", 1L, "DISMISSED", 1L));
        when(metricsRepository.countBySeverity(from, to, null))
                .thenReturn(Map.of("HIGH", 2L, "MEDIUM", 1L, "LOW", 1L));
        when(metricsRepository.countTransactions(from, to)).thenReturn(20L);
        when(metricsRepository.averageAcknowledge(from, to, null))
                .thenReturn(new AlertMetricsRepository.DurationAggregate(3L, 120.0));
        when(metricsRepository.averageResolution(from, to, null))
                .thenReturn(new AlertMetricsRepository.AverageResolutionAggregate(2L, 900.0));
        when(metricsRepository.getSlaAggregate(from, to, null))
                .thenReturn(new AlertMetricsRepository.SlaAggregate(4L, 1L));
        when(metricsRepository.getResolutionOutcomes(from, to, null))
                .thenReturn(new AlertMetricsRepository.ResolutionOutcomeAggregate(2L, 1L));
        when(metricsRepository.countCreatedAlertsByDay(from, to, null)).thenReturn(List.of(
                new AlertMetricsRepository.DailyAlertCount(LocalDate.of(2026, 7, 24), 2L)
        ));
        when(metricsRepository.countTransactionsByDay(from, to)).thenReturn(List.of(
                new AlertMetricsRepository.DailyAlertCount(LocalDate.of(2026, 7, 25), 5L)
        ));
        when(metricsRepository.averageAcknowledgeByDay(from, to, null)).thenReturn(List.of(
                new AlertMetricsRepository.DailyDuration(LocalDate.of(2026, 7, 26), 120.0)
        ));
        when(metricsRepository.averageResolutionByDay(from, to, null)).thenReturn(List.of(
                new AlertMetricsRepository.DailyDuration(LocalDate.of(2026, 7, 27), 900.0)
        ));
        when(metricsRepository.getDailySlaCounts(from, to, null)).thenReturn(List.of(
                new AlertMetricsRepository.DailySlaCount(LocalDate.of(2026, 7, 28), 2L, 1L)
        ));
        when(metricsRepository.countAlertsByRule(from, to, null)).thenReturn(List.of(
                new AlertMetricsRepository.RuleAlertAggregate(7L, "Rapid Transfers", 3L)
        ));

        AlertDashboardMetricsResponse response = metricsService.getDashboardMetrics(7, null);

        assertEquals(4L, response.getKpis().totalAlerts());
        assertEquals(20L, response.getKpis().totalTransactions());
        assertEquals(20.0, response.getKpis().alertTriggerRatePercent());
        assertEquals(25.0, response.getKpis().slaBreachRatePercent());
        assertEquals(50.0, response.getKpis().falsePositiveRatePercent());
        assertEquals(7, response.getAlertTrend().size());
        assertEquals(0L, response.getAlertTrend().get(1).getCount());
        assertEquals(5L, response.getTransactionTrend().get(1).count());
        assertEquals(120.0, response.getResponseTimeTrend().get(2).averageAcknowledgeSeconds());
        assertEquals(900.0, response.getResponseTimeTrend().get(3).averageResolutionSeconds());
        assertEquals(50.0, response.getSlaTrend().get(4).breachRatePercent());
        assertEquals("Rapid Transfers", response.getAlertsByRule().get(0).ruleName());
    }

    @Test
    void dashboardMetrics_shouldUseInclusiveCustomUtcDateRange() {
        LocalDate fromDate = LocalDate.of(2026, 6, 15);
        LocalDate toDate = LocalDate.of(2026, 7, 30);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = LocalDate.of(2026, 7, 31).atStartOfDay();
        stubEmptyDashboardMetrics(from, to, "HIGH");

        AlertDashboardMetricsResponse response =
                metricsService.getDashboardMetrics(fromDate, toDate, " high ");

        assertEquals(46, response.getDays());
        assertEquals(46, response.getAlertTrend().size());
        assertEquals(fromDate, response.getAlertTrend().get(0).getDate());
        assertEquals(toDate, response.getAlertTrend().get(45).getDate());
        assertEquals(Instant.parse("2026-06-15T00:00:00Z"), response.getFrom());
        assertEquals(Instant.parse("2026-07-31T00:00:00Z"), response.getTo());
        assertEquals("HIGH", response.getSeverity());
        verify(metricsRepository).countTransactions(from, to);
    }

    @Test
    void dashboardMetrics_shouldValidateCustomDateRange() {
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> metricsService.getDashboardMetrics(LocalDate.of(2026, 7, 1), null, null));
        assertEquals("from and to are both required for a custom range", missing.getMessage());

        IllegalArgumentException reversed = assertThrows(IllegalArgumentException.class,
                () -> metricsService.getDashboardMetrics(
                        LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 1), null));
        assertEquals("from must not be after to", reversed.getMessage());

        IllegalArgumentException future = assertThrows(IllegalArgumentException.class,
                () -> metricsService.getDashboardMetrics(
                        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null));
        assertEquals("to must not be after the current UTC date", future.getMessage());

        IllegalArgumentException tooLong = assertThrows(IllegalArgumentException.class,
                () -> metricsService.getDashboardMetrics(
                        LocalDate.of(2025, 7, 29), LocalDate.of(2026, 7, 30), null));
        assertEquals("custom dashboard range must not exceed 366 days", tooLong.getMessage());
    }

    private void stubEmptyDashboardMetrics(LocalDateTime from,
                                           LocalDateTime to,
                                           String severity) {
        when(metricsRepository.countByStatus(from, to, severity)).thenReturn(Map.of());
        when(metricsRepository.countBySeverity(from, to, severity)).thenReturn(Map.of());
        when(metricsRepository.countTransactions(from, to)).thenReturn(0L);
        when(metricsRepository.averageAcknowledge(from, to, severity))
                .thenReturn(new AlertMetricsRepository.DurationAggregate(0L, null));
        when(metricsRepository.averageResolution(from, to, severity))
                .thenReturn(new AlertMetricsRepository.AverageResolutionAggregate(0L, null));
        when(metricsRepository.getSlaAggregate(from, to, severity))
                .thenReturn(new AlertMetricsRepository.SlaAggregate(0L, 0L));
        when(metricsRepository.getResolutionOutcomes(from, to, severity))
                .thenReturn(new AlertMetricsRepository.ResolutionOutcomeAggregate(0L, 0L));
        when(metricsRepository.countCreatedAlertsByDay(from, to, severity)).thenReturn(List.of());
        when(metricsRepository.countTransactionsByDay(from, to)).thenReturn(List.of());
        when(metricsRepository.averageAcknowledgeByDay(from, to, severity)).thenReturn(List.of());
        when(metricsRepository.averageResolutionByDay(from, to, severity)).thenReturn(List.of());
        when(metricsRepository.getDailySlaCounts(from, to, severity)).thenReturn(List.of());
        when(metricsRepository.countAlertsByRule(from, to, severity)).thenReturn(List.of());
    }
}
