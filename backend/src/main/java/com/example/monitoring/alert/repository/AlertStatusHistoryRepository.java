package com.example.monitoring.alert.repository;

import com.example.monitoring.alert.entity.AlertStatusHistory;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AlertStatusHistoryRepository extends CrudRepository<AlertStatusHistory, Long> {

    List<AlertStatusHistory> findByAlertId(Long alertId);
}
