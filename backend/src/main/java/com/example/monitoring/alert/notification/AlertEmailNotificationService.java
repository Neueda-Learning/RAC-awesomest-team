package com.example.monitoring.alert.notification;

import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertEmailNotification;
import com.example.monitoring.alert.entity.AlertEmailNotificationStatus;
import com.example.monitoring.alert.repository.AlertEmailNotificationRepository;
import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.rule.entity.MonitoringRule;
import com.example.monitoring.rule.repository.MonitoringRuleRepository;
import com.example.monitoring.transaction.entity.Transaction;
import com.example.monitoring.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AlertEmailNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(AlertEmailNotificationService.class);
    private static final int MAX_ERROR_LENGTH = 2_000;

    private final AlertEmailSettingsService settingsService;
    private final AlertSmtpSender smtpSender;
    private final AlertEmailNotificationRepository notificationRepository;
    private final AlertRepository alertRepository;
    private final TransactionRepository transactionRepository;
    private final MonitoringRuleRepository ruleRepository;

    public AlertEmailNotificationService(
            AlertEmailSettingsService settingsService,
            AlertSmtpSender smtpSender,
            AlertEmailNotificationRepository notificationRepository,
            AlertRepository alertRepository,
            TransactionRepository transactionRepository,
            MonitoringRuleRepository ruleRepository) {
        this.settingsService = settingsService;
        this.smtpSender = smtpSender;
        this.notificationRepository = notificationRepository;
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
        this.ruleRepository = ruleRepository;
    }

    public void queueAndSend(Long alertId) {
        AlertEmailSettingsSnapshot settings = settingsService.getEffectiveSettings();
        if (!settings.enabled()) {
            return;
        }

        try {
            Alert alert = alertRepository.findById(alertId)
                    .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
            if (!"HIGH".equalsIgnoreCase(alert.getSeverity())) {
                return;
            }

            AlertEmailNotification notification = notificationRepository.findByAlertId(alertId)
                    .orElseGet(() -> notificationRepository.save(
                            new AlertEmailNotification(alertId, recipientForAudit(settings))));
            if (notification.getStatus() == AlertEmailNotificationStatus.SENT
                    || attempts(notification) >= settings.maxAttempts()) {
                return;
            }
            attemptDelivery(notification, alert, settings);
        } catch (RuntimeException exception) {
            log.error("Unable to queue HIGH alert email for alert {}", alertId, exception);
        }
    }

    @Scheduled(fixedDelayString = "${app.alert.email.retry-scan-ms:5000}",
            initialDelayString = "${app.alert.email.retry-scan-ms:5000}")
    public void retryFailedNotifications() {
        AlertEmailSettingsSnapshot settings = settingsService.getEffectiveSettings();
        if (!settings.enabled()) {
            return;
        }

        for (AlertEmailNotification notification
                : notificationRepository.findRetryable(
                        settings.maxAttempts(),
                        LocalDateTime.now().minusNanos(settings.retryDelayMs() * 1_000_000L))) {
            try {
                Alert alert = alertRepository.findById(notification.getAlertId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Alert not found: " + notification.getAlertId()));
                attemptDelivery(notification, alert, settings);
            } catch (RuntimeException exception) {
                log.error("Unable to retry HIGH alert email notification {}",
                        notification.getId(), exception);
            }
        }
    }

    private void attemptDelivery(AlertEmailNotification notification,
                                 Alert alert,
                                 AlertEmailSettingsSnapshot settings) {
        if (attempts(notification) >= settings.maxAttempts()) {
            return;
        }

        LocalDateTime attemptedAt = LocalDateTime.now();
        if (settings.toAddress() != null && !settings.toAddress().isBlank()) {
            notification.setRecipient(settings.toAddress().trim());
        }
        notification.setAttemptCount(attempts(notification) + 1);
        notification.setLastAttemptAt(attemptedAt);
        notification.setUpdatedAt(attemptedAt);
        notification.setStatus(AlertEmailNotificationStatus.PENDING);
        notification.setErrorMessage(null);
        notificationRepository.save(notification);

        try {
            settingsService.validateForDelivery(settings);
            Transaction transaction = transactionRepository.findById(alert.getTransactionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Transaction not found: " + alert.getTransactionId()));
            MonitoringRule rule = ruleRepository.findById(alert.getRuleId()).orElse(null);

            smtpSender.send(settings, buildMessage(notification, alert, transaction, rule, settings));

            LocalDateTime sentAt = LocalDateTime.now();
            notification.setStatus(AlertEmailNotificationStatus.SENT);
            notification.setSentAt(sentAt);
            notification.setUpdatedAt(sentAt);
            notification.setErrorMessage(null);
            notificationRepository.save(notification);
            log.info("Sent HIGH alert email for alert {} to {}",
                    alert.getId(), notification.getRecipient());
        } catch (RuntimeException exception) {
            notification.setStatus(AlertEmailNotificationStatus.FAILED);
            notification.setUpdatedAt(LocalDateTime.now());
            notification.setErrorMessage(truncateError(exception));
            notificationRepository.save(notification);
            log.warn("HIGH alert email attempt {} failed for alert {}: {}",
                    notification.getAttemptCount(), alert.getId(), exception.getMessage());
        }
    }

    private SimpleMailMessage buildMessage(AlertEmailNotification notification,
                                           Alert alert,
                                           Transaction transaction,
                                           MonitoringRule rule,
                                           AlertEmailSettingsSnapshot settings) {
        String ruleName = rule == null ? "Rule #" + alert.getRuleId() : rule.getRuleName();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.fromAddress().trim());
        message.setTo(notification.getRecipient().trim());
        message.setSubject("[HIGH ALERT] #" + alert.getId() + " " + ruleName
                + " - " + alert.getAccountId());
        message.setText("""
                A new HIGH-severity transaction monitoring alert was created.

                Alert ID: %s
                Rule: %s (#%s)
                Account: %s
                Severity: %s
                Status: %s
                Alert Created: %s
                SLA Acknowledge Due: %s
                SLA Resolution Due: %s

                Latest Triggering Transaction
                Transaction ID: %s
                Payee: %s
                Amount: %s %s
                Type: %s
                Description: %s
                Transaction Time: %s
                """.formatted(
                alert.getId(),
                ruleName,
                alert.getRuleId(),
                value(alert.getAccountId()),
                value(alert.getSeverity()),
                value(alert.getStatus()),
                value(alert.getCreatedAt()),
                value(alert.getAckDueAt()),
                value(alert.getResolveDueAt()),
                transaction.getId(),
                value(transaction.getPayeeId()),
                value(transaction.getAmount()),
                value(transaction.getCurrency()),
                value(transaction.getTransactionType()),
                value(transaction.getDescription()),
                value(transaction.getCreatedAt())
        ));
        return message;
    }

    private int attempts(AlertEmailNotification notification) {
        return notification.getAttemptCount() == null ? 0 : notification.getAttemptCount();
    }

    private String recipientForAudit(AlertEmailSettingsSnapshot settings) {
        return settings.toAddress() == null || settings.toAddress().isBlank()
                ? "(not configured)"
                : settings.toAddress().trim();
    }

    private String truncateError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }

    private String value(Object value) {
        return value == null ? "-" : value.toString();
    }
}
