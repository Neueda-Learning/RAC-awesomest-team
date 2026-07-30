package com.example.monitoring.alert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlertEmailSettingsRequest(
        @NotNull Boolean enabled,
        @NotBlank @Email @Size(max = 320) String fromAddress,
        @NotBlank @Email @Size(max = 320) String toAddress,
        @NotBlank @Size(max = 255) String smtpHost,
        @NotNull @Min(1) @Max(65535) Integer smtpPort,
        @Size(max = 320) String smtpUsername,
        @NotNull Boolean smtpAuth,
        @NotNull Boolean starttlsEnabled,
        @NotNull Boolean starttlsRequired,
        @NotNull @Min(1) @Max(10) Integer maxAttempts,
        @NotNull @Min(5000) @Max(86400000) Long retryDelayMs
) {
}
