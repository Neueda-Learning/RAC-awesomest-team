package com.example.monitoring.alert.repository;

import com.example.monitoring.alert.dto.AlertTransactionItem;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AlertTransactionLinkRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AlertTransactionLinkRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean addLink(Long alertId, Long transactionId, LocalDateTime triggeredAt) {
        String sql = "INSERT INTO alert_transaction_link (alert_id, transaction_id, triggered_at) "
                + "VALUES (:alertId, :transactionId, :triggeredAt) "
                + "ON DUPLICATE KEY UPDATE triggered_at = triggered_at";
        int changedRows = jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("alertId", alertId)
                .addValue("transactionId", transactionId)
                .addValue("triggeredAt", triggeredAt));
        return changedRows == 1;
    }

    public List<AlertTransactionItem> findByAlertId(Long alertId) {
        String sql = "SELECT t.id, t.account_id, t.payee_id, t.amount, t.currency, "
                + "t.transaction_type, t.description, t.created_at, atl.triggered_at "
                + "FROM alert_transaction_link atl "
                + "JOIN transaction t ON t.id = atl.transaction_id "
                + "WHERE atl.alert_id = :alertId "
                + "ORDER BY atl.triggered_at DESC, t.id DESC";
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("alertId", alertId),
                (rs, rowNum) -> new AlertTransactionItem(
                        rs.getLong("id"),
                        rs.getString("account_id"),
                        rs.getString("payee_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("currency"),
                        rs.getString("transaction_type"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("triggered_at").toLocalDateTime()
                )
        );
    }
}
