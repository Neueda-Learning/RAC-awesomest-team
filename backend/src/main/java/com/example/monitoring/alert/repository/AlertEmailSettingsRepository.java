package com.example.monitoring.alert.repository;

import com.example.monitoring.alert.entity.AlertEmailSettings;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AlertEmailSettingsRepository extends CrudRepository<AlertEmailSettings, Long> {

    @Query("SELECT * FROM alert_email_settings ORDER BY id ASC LIMIT 1")
    Optional<AlertEmailSettings> findCurrent();
}
