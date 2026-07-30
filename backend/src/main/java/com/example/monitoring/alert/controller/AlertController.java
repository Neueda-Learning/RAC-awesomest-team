package com.example.monitoring.alert.controller;

import com.example.monitoring.alert.dto.AlertAverageResolutionResponse;
import com.example.monitoring.alert.dto.AlertDashboardMetricsResponse;
import com.example.monitoring.alert.dto.AlertMetricsSummaryResponse;
import com.example.monitoring.alert.dto.AlertQueryRequest;
import com.example.monitoring.alert.dto.AlertQueryResponse;
import com.example.monitoring.alert.dto.AlertTrendResponse;
import com.example.monitoring.alert.dto.AlertTransactionItem;
import com.example.monitoring.alert.dto.BulkAlertStatusRequest;
import com.example.monitoring.alert.dto.BulkAlertStatusResponse;
import com.example.monitoring.alert.dto.UpdateAlertStatusRequest;
import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertStatus;
import com.example.monitoring.alert.entity.AlertStatusHistory;
import com.example.monitoring.alert.service.AlertMetricsService;
import com.example.monitoring.alert.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService alertService;
    private final AlertMetricsService alertMetricsService;

    public AlertController(AlertService alertService, AlertMetricsService alertMetricsService) {
        this.alertService = alertService;
        this.alertMetricsService = alertMetricsService;
    }

    // GET /alerts — 查看所有告警
    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    // GET /alerts/query — 高级条件查询（后端分页/排序/过滤）
    @GetMapping("/query")
    public ResponseEntity<AlertQueryResponse> queryAlerts(@ModelAttribute AlertQueryRequest request) {
        return ResponseEntity.ok(alertService.queryAlerts(request));
    }

    // GET /alerts/sla/breached — 查询超时告警（支持分页与组合筛选）
    @GetMapping("/sla/breached")
    public ResponseEntity<AlertQueryResponse> querySlaBreachedAlerts(@ModelAttribute AlertQueryRequest request) {
        return ResponseEntity.ok(alertService.querySlaBreachedAlerts(request));
    }

    // POST /alerts/bulk/status — apply one lifecycle action to up to 100 alerts.
    @PostMapping("/bulk/status")
    public ResponseEntity<BulkAlertStatusResponse> bulkChangeStatus(
            @Valid @RequestBody BulkAlertStatusRequest request) {
        return ResponseEntity.ok(alertService.bulkChangeStatus(request));
    }

    // GET /alerts/export — filtered UTF-8 CSV export (maximum 5,000 alerts).
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportAlerts(
            @ModelAttribute AlertQueryRequest request,
            @RequestParam(defaultValue = "csv") String format) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("format must be csv");
        }

        byte[] csv = alertService.exportAlertsCsv(request);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"alerts-" + timestamp + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    // Average creation-to-resolution time for alerts resolved within the UTC reporting range.
    @GetMapping("/metrics/average-resolution")
    public ResponseEntity<AlertAverageResolutionResponse> getAverageResolution(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String severity) {
        return ResponseEntity.ok(alertMetricsService.getAverageResolution(from, to, severity));
    }

    // UTC day buckets for the most recent 7 or 30 days, including days with no alerts.
    @GetMapping("/metrics/trend")
    public ResponseEntity<AlertTrendResponse> getRecentTrend(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String severity) {
        return ResponseEntity.ok(alertMetricsService.getRecentTrend(days, severity));
    }

    // Compact server-side counts used by dashboard cards and distribution charts.
    @GetMapping("/metrics/summary")
    public ResponseEntity<AlertMetricsSummaryResponse> getMetricsSummary(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String severity) {
        return ResponseEntity.ok(alertMetricsService.getSummary(from, to, severity));
    }

    // Complete compact payload for all dashboard cards and charts.
    @GetMapping("/metrics/dashboard")
    public ResponseEntity<AlertDashboardMetricsResponse> getDashboardMetrics(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String severity) {
        if (from != null || to != null) {
            return ResponseEntity.ok(alertMetricsService.getDashboardMetrics(from, to, severity));
        }
        return ResponseEntity.ok(alertMetricsService.getDashboardMetrics(days, severity));
    }

    // GET /alerts?status=OPEN — 按状态筛选告警
    @GetMapping(params = "status")
    public ResponseEntity<List<Alert>> getAlertsByStatus(@RequestParam AlertStatus status) {
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }

    // GET /alerts/{id} — 查看单个告警详情
    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        return alertService.getAlertById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /alerts/{id}/detail — 明确的详情接口（用于列表行展开）
    @GetMapping("/{id}/detail")
    public ResponseEntity<Alert> getAlertDetailById(@PathVariable Long id) {
        return alertService.getAlertDetailById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /alerts/{id}/history — 查看告警状态历史
    @GetMapping("/{id}/history")
    public ResponseEntity<List<AlertStatusHistory>> getAlertHistory(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAlertHistory(id));
    }

    // GET /alerts/{id}/transactions — all transactions represented by this deduplicated alert.
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<AlertTransactionItem>> getAlertTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAlertTransactions(id));
    }

    // PATCH /alerts/{id}/acknowledge — 确认告警 (OPEN → ACKNOWLEDGED)
    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledge(@PathVariable Long id,
                                             @RequestBody(required = false) UpdateAlertStatusRequest request) {
        String notes = request != null ? request.getNotes() : null;
        return ResponseEntity.ok(alertService.acknowledge(id, notes));
    }

    // PATCH /alerts/{id}/investigate — 开始调查 (ACKNOWLEDGED → INVESTIGATING)
    @PatchMapping("/{id}/investigate")
    public ResponseEntity<Alert> investigate(@PathVariable Long id,
                                             @RequestBody(required = false) UpdateAlertStatusRequest request) {
        String notes = request != null ? request.getNotes() : null;
        return ResponseEntity.ok(alertService.startInvestigating(id, notes));
    }

    // PATCH /alerts/{id}/close — 关闭告警 (INVESTIGATING → CLOSED)
    @PatchMapping("/{id}/close")
    public ResponseEntity<Alert> close(@PathVariable Long id,
                                       @RequestBody(required = false) UpdateAlertStatusRequest request) {
        String notes = request != null ? request.getNotes() : null;
        return ResponseEntity.ok(alertService.close(id, notes));
    }

    // PATCH /alerts/{id}/dismiss — 驳回告警 (ACKNOWLEDGED/INVESTIGATING → DISMISSED)
    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<Alert> dismiss(@PathVariable Long id,
                                         @RequestBody(required = false) UpdateAlertStatusRequest request) {
        String notes = request != null ? request.getNotes() : null;
        return ResponseEntity.ok(alertService.dismiss(id, notes));
    }
}
