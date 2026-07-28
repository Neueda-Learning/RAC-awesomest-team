package com.example.monitoring.alert.repository;

import com.example.monitoring.alert.entity.Alert;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AlertRepository extends CrudRepository<Alert, Long> {

    List<Alert> findByStatus(String status);

    List<Alert> findByAccountId(String accountId);

    List<Alert> findBySeverity(String severity);
}
