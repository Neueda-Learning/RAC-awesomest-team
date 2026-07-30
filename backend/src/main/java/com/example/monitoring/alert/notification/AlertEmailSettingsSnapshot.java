package com.example.monitoring.alert.notification;

import java.time.LocalDateTime;

public record AlertEmailSettingsSnapshot(
        boolean enabled,
        String fromAddress,
        String toAddress,
        String smtpHost,
        int smtpPort,
        String smtpUsername,
        boolean smtpAuth,
        boolean starttlsEnabled,
        boolean starttlsRequired,
        int maxAttempts,
        long retryDelayMs,
        boolean persisted,
        LocalDateTime updatedAt
) {
}
