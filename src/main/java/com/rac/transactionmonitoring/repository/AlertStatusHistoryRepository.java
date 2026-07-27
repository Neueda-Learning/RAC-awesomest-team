package com.rac.transactionmonitoring.repository;

import com.rac.transactionmonitoring.model.AlertStatusHistory;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AlertStatusHistoryRepository extends CrudRepository<AlertStatusHistory, Long> {

    List<AlertStatusHistory> findByAlertId(Long alertId);
}

