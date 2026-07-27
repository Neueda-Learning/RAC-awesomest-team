package com.example.monitoring.rule.controller;

import com.example.monitoring.rule.entity.MonitoringRule;
import com.example.monitoring.rule.service.RuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    // GET /rules — 查看所有规则
    @GetMapping
    public ResponseEntity<List<MonitoringRule>> getAllRules() {
        return ResponseEntity.ok(ruleService.getAllRules());
    }

    // GET /rules/{id} — 查看单个规则
    @GetMapping("/{id}")
    public ResponseEntity<MonitoringRule> getRuleById(@PathVariable Long id) {
        return ruleService.getRuleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /rules — 创建新规则
    @PostMapping
    public ResponseEntity<MonitoringRule> createRule(@RequestBody MonitoringRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.createRule(rule));
    }

    // PUT /rules/{id} — 更新规则
    @PutMapping("/{id}")
    public ResponseEntity<MonitoringRule> updateRule(@PathVariable Long id,
                                                      @RequestBody MonitoringRule rule) {
        return ResponseEntity.ok(ruleService.updateRule(id, rule));
    }

    // DELETE /rules/{id} — 删除规则
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
