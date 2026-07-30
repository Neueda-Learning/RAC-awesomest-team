package com.example.monitoring.alert.repository;

import com.example.monitoring.alert.entity.AlertEmailNotification;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertEmailNotificationRepository
        extends CrudRepository<AlertEmailNotification, Long> {

    Optional<AlertEmailNotification> findByAlertId(Long alertId);

    @Query("SELECT * FROM alert_email_notification "
            + "WHERE status IN ('PENDING', 'FAILED') "
            + "AND attempt_count < :maxAttempts "
            + "AND (last_attempt_at IS NULL OR last_attempt_at <= :retryBefore) "
            + "ORDER BY updated_at ASC LIMIT 100")
    List<AlertEmailNotification> findRetryable(
            @Param("maxAttempts") int maxAttempts,
            @Param("retryBefore") LocalDateTime retryBefore);
}
