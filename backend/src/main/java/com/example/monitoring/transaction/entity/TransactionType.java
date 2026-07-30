package com.example.monitoring.transaction.entity;

import java.util.Locale;

public enum TransactionType {
    SALARY,
    REFUND,
    TRANSFER_OUT,
    DEPOSIT,
    WITHDRAWAL;

    public static TransactionType from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("transactionType is required");
        }

        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if ("DEBIT".equals(normalized)) {
            return TRANSFER_OUT;
        }
        if ("CREDIT".equals(normalized)) {
            return DEPOSIT;
        }

        try {
            return TransactionType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported transactionType: " + rawValue);
        }
    }

    public boolean requiresPayee() {
        return this == SALARY || this == REFUND || this == TRANSFER_OUT;
    }

    public boolean forbidsPayee() {
        return this == DEPOSIT || this == WITHDRAWAL;
    }

    public boolean isAlertExempt() {
        return this == SALARY || this == REFUND;
    }
}
