package com.rac.transactionmonitoring.controller;

import com.rac.transactionmonitoring.dto.CreateTransactionRequest;
import com.rac.transactionmonitoring.model.Transaction;
import com.rac.transactionmonitoring.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

