package com.example.monitoring.alert;

import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertEmailNotification;
import com.example.monitoring.alert.entity.AlertEmailNotificationStatus;
import com.example.monitoring.alert.notification.AlertEmailNotificationService;
import com.example.monitoring.alert.notification.AlertEmailSettingsService;
import com.example.monitoring.alert.notification.AlertEmailSettingsSnapshot;
import com.example.monitoring.alert.notification.AlertSmtpSender;
import com.example.monitoring.alert.repository.AlertEmailNotificationRepository;
import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.rule.entity.MonitoringRule;
import com.example.monitoring.rule.repository.MonitoringRuleRepository;
import com.example.monitoring.transaction.entity.Transaction;
import com.example.monitoring.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AlertEmailNotificationServiceTest {

    @Mock
    private AlertEmailSettingsService settingsService;

    @Mock
    private AlertSmtpSender smtpSender;

    @Mock
    private AlertEmailNotificationRepository notificationRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MonitoringRuleRepository ruleRepository;

    private AlertEmailNotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(settingsService.getEffectiveSettings()).thenReturn(settings(true, "admin@example.com"));
        notificationService = new AlertEmailNotificationService(
                settingsService,
                smtpSender,
                notificationRepository,
                alertRepository,
                transactionRepository,
                ruleRepository
        );
        lenient().when(notificationRepository.save(any(AlertEmailNotification.class)))
                .thenAnswer(invocation -> {
                    AlertEmailNotification notification = invocation.getArgument(0);
                    if (notification.getId() == null) {
                        notification.setId(500L);
                    }
                    return notification;
                });
    }

    @Test
    void queueAndSend_shouldSendAndRecordHighAlert() {
        Alert alert = buildHighAlert();
        Transaction transaction = buildTransaction();
        MonitoringRule rule = buildRule();
        when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(notificationRepository.findByAlertId(10L)).thenReturn(Optional.empty());
        when(transactionRepository.findById(20L)).thenReturn(Optional.of(transaction));
        when(ruleRepository.findById(4L)).thenReturn(Optional.of(rule));

        notificationService.queueAndSend(10L);

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtpSender).send(any(AlertEmailSettingsSnapshot.class), messageCaptor.capture());
        assertEquals("monitoring@example.com", messageCaptor.getValue().getFrom());
        assertEquals("admin@example.com", messageCaptor.getValue().getTo()[0]);
        assertTrue(messageCaptor.getValue().getSubject().contains("[HIGH ALERT] #10"));
        assertTrue(messageCaptor.getValue().getText().contains("Transaction ID: 20"));

        ArgumentCaptor<AlertEmailNotification> notificationCaptor =
                ArgumentCaptor.forClass(AlertEmailNotification.class);
        verify(notificationRepository, atLeast(3)).save(notificationCaptor.capture());
        AlertEmailNotification finalState = notificationCaptor.getValue();
        assertEquals(AlertEmailNotificationStatus.SENT, finalState.getStatus());
        assertEquals(1, finalState.getAttemptCount());
    }

    @Test
    void queueAndSend_shouldRecordFailureWithoutThrowing() {
        Alert alert = buildHighAlert();
        when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(notificationRepository.findByAlertId(10L)).thenReturn(Optional.empty());
        when(transactionRepository.findById(20L)).thenReturn(Optional.of(buildTransaction()));
        when(ruleRepository.findById(4L)).thenReturn(Optional.of(buildRule()));
        doThrow(new MailSendException("SMTP unavailable")).when(smtpSender)
                .send(any(AlertEmailSettingsSnapshot.class), any(SimpleMailMessage.class));

        notificationService.queueAndSend(10L);

        ArgumentCaptor<AlertEmailNotification> notificationCaptor =
                ArgumentCaptor.forClass(AlertEmailNotification.class);
        verify(notificationRepository, atLeast(3)).save(notificationCaptor.capture());
        AlertEmailNotification finalState = notificationCaptor.getValue();
        assertEquals(AlertEmailNotificationStatus.FAILED, finalState.getStatus());
        assertEquals(1, finalState.getAttemptCount());
        assertTrue(finalState.getErrorMessage().contains("SMTP unavailable"));
    }

    @Test
    void queueAndSend_shouldDoNothingWhenDisabled() {
        when(settingsService.getEffectiveSettings()).thenReturn(settings(false, "admin@example.com"));

        notificationService.queueAndSend(10L);

        verify(alertRepository, never()).findById(any(Long.class));
        verify(smtpSender, never()).send(
                any(AlertEmailSettingsSnapshot.class), any(SimpleMailMessage.class));
    }

    @Test
    void queueAndSend_shouldRecordMissingRecipientConfiguration() {
        when(settingsService.getEffectiveSettings()).thenReturn(settings(true, ""));
        doThrow(new IllegalStateException("Recipient address is required"))
                .when(settingsService).validateForDelivery(any(AlertEmailSettingsSnapshot.class));
        when(alertRepository.findById(10L)).thenReturn(Optional.of(buildHighAlert()));
        when(notificationRepository.findByAlertId(10L)).thenReturn(Optional.empty());

        notificationService.queueAndSend(10L);

        ArgumentCaptor<AlertEmailNotification> notificationCaptor =
                ArgumentCaptor.forClass(AlertEmailNotification.class);
        verify(notificationRepository, atLeast(3)).save(notificationCaptor.capture());
        AlertEmailNotification finalState = notificationCaptor.getValue();
        assertEquals(AlertEmailNotificationStatus.FAILED, finalState.getStatus());
        assertEquals("(not configured)", finalState.getRecipient());
        assertTrue(finalState.getErrorMessage().contains("Recipient address is required"));
        verify(smtpSender, never()).send(
                any(AlertEmailSettingsSnapshot.class), any(SimpleMailMessage.class));
    }

    @Test
    void retryFailedNotifications_shouldRetryUntilMaximum() {
        AlertEmailNotification failed = new AlertEmailNotification(10L, "admin@example.com");
        failed.setId(500L);
        failed.setStatus(AlertEmailNotificationStatus.FAILED);
        failed.setAttemptCount(1);
        when(notificationRepository.findRetryable(anyInt(), any(LocalDateTime.class)))
                .thenReturn(List.of(failed));
        when(alertRepository.findById(10L)).thenReturn(Optional.of(buildHighAlert()));
        when(transactionRepository.findById(20L)).thenReturn(Optional.of(buildTransaction()));
        when(ruleRepository.findById(4L)).thenReturn(Optional.of(buildRule()));

        notificationService.retryFailedNotifications();

        verify(smtpSender).send(
                any(AlertEmailSettingsSnapshot.class), any(SimpleMailMessage.class));
        assertEquals(AlertEmailNotificationStatus.SENT, failed.getStatus());
        assertEquals(2, failed.getAttemptCount());
    }

    private Alert buildHighAlert() {
        Alert alert = new Alert(4L, 20L, "ACC-001", "HIGH");
        alert.setId(10L);
        alert.setCreatedAt(LocalDateTime.of(2026, 7, 30, 12, 0));
        return alert;
    }

    private Transaction buildTransaction() {
        Transaction transaction = new Transaction(
                "ACC-001",
                "PAY-001",
                new BigDecimal("75000.00"),
                "USD",
                "TRANSFER_OUT",
                "high risk transfer",
                LocalDateTime.of(2026, 7, 30, 12, 0)
        );
        transaction.setId(20L);
        return transaction;
    }

    private MonitoringRule buildRule() {
        MonitoringRule rule = new MonitoringRule();
        rule.setId(4L);
        rule.setRuleName("Daily Limit Exceeded");
        rule.setSeverity("HIGH");
        return rule;
    }

    private AlertEmailSettingsSnapshot settings(boolean enabled, String recipient) {
        return new AlertEmailSettingsSnapshot(
                enabled,
                "monitoring@example.com",
                recipient,
                "smtp.example.com",
                587,
                "monitoring@example.com",
                true,
                true,
                true,
                3,
                60_000L,
                true,
                LocalDateTime.of(2026, 7, 30, 11, 0));
    }
}
