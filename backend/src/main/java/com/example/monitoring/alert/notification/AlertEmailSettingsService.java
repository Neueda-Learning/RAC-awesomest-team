package com.example.monitoring.alert.notification;

import com.example.monitoring.alert.dto.AlertEmailSettingsRequest;
import com.example.monitoring.alert.dto.AlertEmailSettingsResponse;
import com.example.monitoring.alert.dto.AlertEmailTestResponse;
import com.example.monitoring.alert.entity.AlertEmailSettings;
import com.example.monitoring.alert.repository.AlertEmailSettingsRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AlertEmailSettingsService {

    private final AlertEmailSettingsRepository repository;
    private final AlertEmailProperties defaults;
    private final AlertSmtpSender smtpSender;

    public AlertEmailSettingsService(AlertEmailSettingsRepository repository,
                                     AlertEmailProperties defaults,
                                     AlertSmtpSender smtpSender) {
        this.repository = repository;
        this.defaults = defaults;
        this.smtpSender = smtpSender;
    }

    public AlertEmailSettingsResponse getSettings() {
        return toResponse(getEffectiveSettings());
    }

    public AlertEmailSettingsSnapshot getEffectiveSettings() {
        return repository.findCurrent()
                .map(this::toSnapshot)
                .orElseGet(this::defaultSnapshot);
    }

    @Transactional
    public AlertEmailSettingsResponse updateSettings(AlertEmailSettingsRequest request) {
        validateRelationshipRules(request);
        LocalDateTime now = LocalDateTime.now();
        AlertEmailSettings entity = repository.findCurrent().orElseGet(() -> {
            AlertEmailSettings created = new AlertEmailSettings();
            created.setCreatedAt(now);
            return created;
        });

        entity.setEnabled(request.enabled());
        entity.setFromAddress(request.fromAddress().trim());
        entity.setToAddress(request.toAddress().trim());
        entity.setSmtpHost(request.smtpHost().trim());
        entity.setSmtpPort(request.smtpPort());
        entity.setSmtpUsername(trimToEmpty(request.smtpUsername()));
        entity.setSmtpAuth(request.smtpAuth());
        entity.setStarttlsEnabled(request.starttlsEnabled());
        entity.setStarttlsRequired(request.starttlsRequired());
        entity.setMaxAttempts(request.maxAttempts());
        entity.setRetryDelayMs(request.retryDelayMs());
        entity.setUpdatedAt(now);
        return toResponse(toSnapshot(repository.save(entity)));
    }

    public AlertEmailTestResponse sendTestEmail() {
        AlertEmailSettingsSnapshot settings = getEffectiveSettings();
        validateForDelivery(settings);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.fromAddress());
        message.setTo(settings.toAddress());
        message.setSubject("[TEST] Transaction Monitoring email configuration");
        message.setText("""
                This is a test message from the Transaction Monitoring Service.

                SMTP connection and the configured sender/recipient are working.
                No transaction alert was created by this test.
                """);
        smtpSender.send(settings, message);

        return new AlertEmailTestResponse(
                true,
                settings.toAddress(),
                LocalDateTime.now(),
                "Test email sent successfully");
    }

    public void validateForDelivery(AlertEmailSettingsSnapshot settings) {
        if (settings.fromAddress() == null || settings.fromAddress().isBlank()) {
            throw new IllegalStateException("Sender address is required");
        }
        if (settings.toAddress() == null || settings.toAddress().isBlank()) {
            throw new IllegalStateException("Recipient address is required");
        }
        if (settings.smtpHost() == null || settings.smtpHost().isBlank()) {
            throw new IllegalStateException("SMTP host is required");
        }
        if (settings.smtpPort() < 1 || settings.smtpPort() > 65535) {
            throw new IllegalStateException("SMTP port must be between 1 and 65535");
        }
        if (settings.smtpAuth() && (settings.smtpUsername() == null
                || settings.smtpUsername().isBlank())) {
            throw new IllegalStateException("SMTP username is required when authentication is enabled");
        }
        if (settings.smtpAuth() && !smtpSender.isPasswordConfigured()) {
            throw new IllegalStateException(
                    "SMTP_PASSWORD is not configured in the backend environment");
        }
        if (settings.starttlsRequired() && !settings.starttlsEnabled()) {
            throw new IllegalStateException(
                    "STARTTLS must be enabled when it is required");
        }
    }

    private void validateRelationshipRules(AlertEmailSettingsRequest request) {
        if (request.smtpAuth()
                && (request.smtpUsername() == null || request.smtpUsername().isBlank())) {
            throw new IllegalArgumentException(
                    "smtpUsername is required when smtpAuth is enabled");
        }
        if (request.starttlsRequired() && !request.starttlsEnabled()) {
            throw new IllegalArgumentException(
                    "starttlsEnabled must be true when starttlsRequired is true");
        }
    }

    private AlertEmailSettingsSnapshot defaultSnapshot() {
        return new AlertEmailSettingsSnapshot(
                defaults.isEnabled(),
                trimToEmpty(defaults.getFrom()),
                trimToEmpty(defaults.getTo()),
                defaultHost(defaults.getSmtpHost()),
                validPort(defaults.getSmtpPort()),
                trimToEmpty(defaults.getSmtpUsername()),
                defaults.isSmtpAuth(),
                defaults.isStarttlsEnabled(),
                defaults.isStarttlsRequired(),
                Math.max(1, defaults.getMaxAttempts()),
                Math.max(5_000L, defaults.getRetryDelayMs()),
                false,
                null);
    }

    private AlertEmailSettingsSnapshot toSnapshot(AlertEmailSettings entity) {
        return new AlertEmailSettingsSnapshot(
                Boolean.TRUE.equals(entity.getEnabled()),
                trimToEmpty(entity.getFromAddress()),
                trimToEmpty(entity.getToAddress()),
                defaultHost(entity.getSmtpHost()),
                validPort(entity.getSmtpPort()),
                trimToEmpty(entity.getSmtpUsername()),
                Boolean.TRUE.equals(entity.getSmtpAuth()),
                Boolean.TRUE.equals(entity.getStarttlsEnabled()),
                Boolean.TRUE.equals(entity.getStarttlsRequired()),
                entity.getMaxAttempts() == null ? 3 : Math.max(1, entity.getMaxAttempts()),
                entity.getRetryDelayMs() == null
                        ? 60_000L
                        : Math.max(5_000L, entity.getRetryDelayMs()),
                true,
                entity.getUpdatedAt());
    }

    private AlertEmailSettingsResponse toResponse(AlertEmailSettingsSnapshot settings) {
        return new AlertEmailSettingsResponse(
                settings.enabled(),
                settings.fromAddress(),
                settings.toAddress(),
                settings.smtpHost(),
                settings.smtpPort(),
                settings.smtpUsername(),
                settings.smtpAuth(),
                settings.starttlsEnabled(),
                settings.starttlsRequired(),
                settings.maxAttempts(),
                settings.retryDelayMs(),
                smtpSender.isPasswordConfigured(),
                settings.persisted(),
                settings.updatedAt());
    }

    private int validPort(Integer value) {
        return value == null || value < 1 || value > 65535 ? 25 : value;
    }

    private String defaultHost(String value) {
        return value == null || value.isBlank() ? "localhost" : value.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
