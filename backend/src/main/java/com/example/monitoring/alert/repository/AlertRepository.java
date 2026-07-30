package com.example.monitoring.alert.repository;

import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertStatus;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface AlertRepository extends CrudRepository<Alert, Long> {

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByAccountId(String accountId);

    List<Alert> findBySeverity(String severity);

    @Query("SELECT * FROM alert "
            + "WHERE rule_id = :ruleId "
            + "AND account_id = :accountId "
            + "AND status IN ('OPEN','ACKNOWLEDGED','INVESTIGATING') "
            + "AND COALESCE(last_triggered_at, created_at) >= :since "
            + "ORDER BY COALESCE(last_triggered_at, created_at) DESC LIMIT 1")
    Optional<Alert> findLatestActiveForDedup(@Param("ruleId") Long ruleId,
                                              @Param("accountId") String accountId,
                                              @Param("since") LocalDateTime since);
}
