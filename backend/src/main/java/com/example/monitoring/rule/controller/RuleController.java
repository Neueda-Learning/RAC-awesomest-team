package com.example.monitoring.rule.controller;

import com.example.monitoring.rule.entity.MonitoringRule;
import com.example.monitoring.rule.entity.RuleCondition;
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

    @GetMapping
    public ResponseEntity<List<MonitoringRule>> getAllRules() {
        return ResponseEntity.ok(ruleService.getAllRules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitoringRule> getRuleById(@PathVariable Long id) {
        return ruleService.getRuleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MonitoringRule> createRule(@RequestBody MonitoringRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.createRule(rule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitoringRule> updateRule(@PathVariable Long id,
                                                      @RequestBody MonitoringRule rule) {
        return ResponseEntity.ok(ruleService.updateRule(id, rule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    // ── Condition endpoints ──────────────────────────────────────────────────

    @GetMapping("/{id}/conditions")
    public ResponseEntity<List<RuleCondition>> getConditions(@PathVariable Long id) {
        return ResponseEntity.ok(ruleService.getConditions(id));
    }

    @PostMapping("/{id}/conditions")
    public ResponseEntity<RuleCondition> addCondition(@PathVariable Long id,
                                                       @RequestBody RuleCondition condition) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.addCondition(id, condition));
    }

    @DeleteMapping("/{id}/conditions")
    public ResponseEntity<Void> deleteAllConditions(@PathVariable Long id) {
        ruleService.deleteAllConditions(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/conditions/{conditionId}")
    public ResponseEntity<Void> deleteCondition(@PathVariable Long id,
                                                 @PathVariable Long conditionId) {
        ruleService.deleteCondition(conditionId);
        return ResponseEntity.noContent().build();
    }
}
