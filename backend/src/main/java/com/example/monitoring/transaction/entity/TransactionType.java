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

        try {
            return TransactionType.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
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

