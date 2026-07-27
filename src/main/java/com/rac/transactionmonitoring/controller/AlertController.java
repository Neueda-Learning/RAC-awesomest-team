package com.rac.transactionmonitoring.controller;

import com.rac.transactionmonitoring.dto.UpdateAlertStatusRequest;
import com.rac.transactionmonitoring.model.Alert;
import com.rac.transactionmonitoring.model.AlertStatusHistory;
import com.rac.transactionmonitoring.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    // GET /alerts — 查看所有告警
    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    // GET /alerts?status=OPEN — 按状态筛选告警
    @GetMapping(params = "status")
    public ResponseEntity<List<Alert>> getAlertsByStatus(@RequestParam String status) {
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }

    // GET /alerts/{id} — 查看单个告警详情
    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        return alertService.getAlertById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /alerts/{id}/history — 查看告警状态历史
    @GetMapping("/{id}/history")
    public ResponseEntity<List<AlertStatusHistory>> getAlertHistory(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAlertHistory(id));
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

    // PATCH /alerts/{id}/close — 关闭告警 (ACKNOWLEDGED/INVESTIGATING → CLOSED)
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

