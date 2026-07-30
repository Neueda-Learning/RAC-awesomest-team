package com.example.monitoring.alert.repository;

import com.example.monitoring.alert.dto.AlertQueryRequest;
import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class AlertQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AlertQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Runs a paginated and filterable alert query using dynamic SQL conditions.
     *
     * @param request validated query request with filtering and sort options
     * @return page payload containing matching alerts and total row count
     */
    public AlertQueryResult query(AlertQueryRequest request) {
        QueryParts queryParts = buildWhereClause(request);
        String orderBy = buildOrderBy(request);

        String selectSql = "SELECT id, rule_id, transaction_id, account_id, severity, status, created_at, updated_at "
                + "FROM alert " + queryParts.whereSql + " " + orderBy + " LIMIT :limit OFFSET :offset";

        MapSqlParameterSource pageParams = new MapSqlParameterSource(queryParts.params.getValues());
        pageParams.addValue("limit", request.getSize());
        pageParams.addValue("offset", request.getPage() * request.getSize());

        List<Alert> content = jdbcTemplate.query(selectSql, pageParams, (rs, rowNum) -> mapAlert(rs));

        String countSql = "SELECT COUNT(*) FROM alert " + queryParts.whereSql;
        Long total = jdbcTemplate.queryForObject(countSql, queryParts.params, Long.class);

        return new AlertQueryResult(content, total == null ? 0L : total);
    }

    private QueryParts buildWhereClause(AlertQueryRequest request) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (request.getStatus() != null) {
            where.append(" AND status = :status");
            params.addValue("status", request.getStatus().name());
        }
        if (request.getSeverity() != null) {
            where.append(" AND severity = :severity");
            params.addValue("severity", request.getSeverity());
        }
        if (request.getAccountId() != null) {
            where.append(" AND account_id = :accountId");
            params.addValue("accountId", request.getAccountId());
        }
        if (request.getRuleId() != null) {
            where.append(" AND rule_id = :ruleId");
            params.addValue("ruleId", request.getRuleId());
        }
        if (request.getFrom() != null) {
            where.append(" AND created_at >= :from");
            params.addValue("from", request.getFrom());
        }
        if (request.getTo() != null) {
            where.append(" AND created_at <= :to");
            params.addValue("to", request.getTo());
        }

        return new QueryParts(where.toString(), params);
    }

    private String buildOrderBy(AlertQueryRequest request) {
        Map<String, String> sortColumns = Map.of(
                "createdAt", "created_at",
                "severity", "severity",
                "status", "status",
                "updatedAt", "updated_at"
        );

        String column = sortColumns.getOrDefault(request.getSortBy(), "created_at");
        String direction = "asc".equalsIgnoreCase(request.getSortDir()) ? "ASC" : "DESC";
        return "ORDER BY " + column + " " + direction;
    }

    private Alert mapAlert(ResultSet rs) throws SQLException {
        Alert alert = new Alert();
        alert.setId(rs.getLong("id"));
        alert.setRuleId(rs.getLong("rule_id"));
        alert.setTransactionId(rs.getLong("transaction_id"));
        alert.setAccountId(rs.getString("account_id"));
        alert.setSeverity(rs.getString("severity"));
        alert.setStatus(AlertStatus.valueOf(rs.getString("status")));
        alert.setCreatedAt(readDateTime(rs, "created_at"));
        alert.setUpdatedAt(readDateTime(rs, "updated_at"));
        return alert;
    }

    private LocalDateTime readDateTime(ResultSet rs, String columnName) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record QueryParts(String whereSql, MapSqlParameterSource params) {
    }

    public static class AlertQueryResult {
        private final List<Alert> content;
        private final long totalElements;

        public AlertQueryResult(List<Alert> content, long totalElements) {
            this.content = new ArrayList<>(content);
            this.totalElements = totalElements;
        }

        public List<Alert> getContent() {
            return content;
        }

        public long getTotalElements() {
            return totalElements;
        }
    }
}

