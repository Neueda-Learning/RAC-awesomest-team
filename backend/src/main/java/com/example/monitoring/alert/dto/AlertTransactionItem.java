package com.example.monitoring.alert.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlertTransactionItem(
        Long transactionId,
        String accountId,
        String payeeId,
        BigDecimal amount,
        String currency,
        String transactionType,
        String description,
        LocalDateTime transactionCreatedAt,
        LocalDateTime alertTriggeredAt) {
}
