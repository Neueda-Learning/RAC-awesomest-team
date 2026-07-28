package com.example.monitoring.transaction.controller;

import com.example.monitoring.transaction.dto.CreateTransactionRequest;
import com.example.monitoring.transaction.entity.Transaction;
import com.example.monitoring.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // POST /transactions — 提交一笔交易（并自动触发规则评估）
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction created = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /transactions — 查看所有交易
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    // GET /transactions/{id} — 查看单笔交易
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /transactions?accountId=ACC-001 — 按账户查询
    @GetMapping(params = "accountId")
    public ResponseEntity<List<Transaction>> getByAccount(@RequestParam String accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId));
    }

    // GET /transactions/search?keyword=wire — 按描述关键词模糊搜索
    @GetMapping("/search")
    public ResponseEntity<List<Transaction>> searchByDescription(@RequestParam String keyword) {
        return ResponseEntity.ok(transactionService.searchByDescription(keyword));
    }

    // GET /transactions/filter?minAmount=100&maxAmount=5000&from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
    // 按金额范围和/或日期区间筛选（参数都是可选的）
    @GetMapping("/filter")
    public ResponseEntity<List<Transaction>> filter(
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(transactionService.filterTransactions(minAmount, maxAmount, from, to));
    }

}
