package com.example.monitoring.alert.service;

import com.example.monitoring.alert.entity.Alert;

import java.time.LocalDateTime;

public final class AlertSlaPolicy {

    private AlertSlaPolicy() {
    }

    public static LocalDateTime calculateAckDueAt(String severity, LocalDateTime baseTime) {
        return switch (normalizeSeverity(severity)) {
            case "HIGH" -> baseTime.plusMinutes(5);
            case "MEDIUM" -> baseTime.plusMinutes(15);
            default -> baseTime.plusHours(1);
        };
    }

    public static LocalDateTime calculateResolveDueAt(String severity, LocalDateTime baseTime) {
        return switch (normalizeSeverity(severity)) {
            case "HIGH" -> baseTime.plusMinutes(30);
            case "MEDIUM" -> baseTime.plusHours(2);
            default -> baseTime.plusHours(24);
        };
    }

    public static boolean isSlaBreached(Alert alert) {
        boolean ackBreached = alert.getAckDueAt() != null
                && alert.getAckAt() != null
                && alert.getAckAt().isAfter(alert.getAckDueAt());

        boolean resolveBreached = alert.getResolveDueAt() != null
                && alert.getResolvedAt() != null
                && alert.getResolvedAt().isAfter(alert.getResolveDueAt());

        return ackBreached || resolveBreached;
    }

    private static String normalizeSeverity(String severity) {
        if (severity == null) {
            return "LOW";
        }
        return severity.trim().toUpperCase();
    }
}

