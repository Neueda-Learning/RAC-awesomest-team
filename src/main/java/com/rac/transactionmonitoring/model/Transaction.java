package com.rac.transactionmonitoring.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("transaction")
public class Transaction {

    @Id
    private Long id;
    private String accountId;
    private String payeeId;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String description;
    private LocalDateTime createdAt;

    public Transaction() {}

    public Transaction(String accountId, String payeeId, BigDecimal amount, String currency,
                       String transactionType, String description, LocalDateTime createdAt) {
        this.accountId = accountId;
        this.payeeId = payeeId;
        this.amount = amount;
        this.currency = currency;
        this.transactionType = transactionType;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getPayeeId() { return payeeId; }
    public void setPayeeId(String payeeId) { this.payeeId = payeeId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

