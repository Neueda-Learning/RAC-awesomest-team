package com.example.monitoring.alert.dto;

import java.time.LocalDateTime;

public record AlertEmailSettingsResponse(
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
        boolean passwordConfigured,
        boolean persisted,
        LocalDateTime updatedAt
) {
}
