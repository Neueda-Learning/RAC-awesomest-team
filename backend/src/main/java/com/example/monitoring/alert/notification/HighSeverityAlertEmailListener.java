package com.example.monitoring.alert.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class HighSeverityAlertEmailListener {

    private final AlertEmailNotificationService notificationService;

    public HighSeverityAlertEmailListener(AlertEmailNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onHighSeverityAlertCreated(HighSeverityAlertCreatedEvent event) {
        notificationService.queueAndSend(event.alertId());
    }
}
