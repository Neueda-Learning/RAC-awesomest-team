package com.example.monitoring.alert.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AlertMetricsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AlertMetricsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AverageResolutionAggregate averageResolution(LocalDateTime from,
                                                        LocalDateTime to,
                                                        String severity) {
        DurationAggregate result = averageDuration("resolved_at", from, to, severity, true);
        return new AverageResolutionAggregate(result.alertCount(), result.averageSeconds());
    }

    public DurationAggregate averageAcknowledge(LocalDateTime from,
                                                LocalDateTime to,
                                                String severity) {
        return averageDuration("ack_at", from, to, severity, false);
    }

    private DurationAggregate averageDuration(String eventColumn,
                                              LocalDateTime from,
                                              LocalDateTime to,
                                              String severity,
                                              boolean terminalOnly) {
        MetricQuery query = buildRangeQuery(eventColumn, from, to, severity);
        String sql = "SELECT COUNT(*) AS resolved_count, "
                + "AVG(TIMESTAMPDIFF(MICROSECOND, created_at, " + eventColumn + ") / 1000000.0) AS average_seconds "
                + "FROM alert "
                + query.whereSql
                + " AND " + eventColumn + " IS NOT NULL"
                + " AND " + eventColumn + " >= created_at"
                + (terminalOnly ? " AND status IN ('CLOSED', 'DISMISSED')" : "");

        return jdbcTemplate.queryForObject(sql, query.params, (rs, rowNum) ->
                new DurationAggregate(
                        rs.getLong("resolved_count"),
                        rs.getObject("average_seconds") == null ? null : rs.getDouble("average_seconds")
                ));
    }

    public List<DailyAlertCount> countCreatedAlertsByDay(LocalDateTime from,
                                                         LocalDateTime to,
                                                         String severity) {
        MetricQuery query = buildRangeQuery("created_at", from, to, severity);
        String sql = "SELECT DATE(created_at) AS bucket_date, COUNT(*) AS alert_count "
                + "FROM alert "
                + query.whereSql
                + " GROUP BY DATE(created_at)"
                + " ORDER BY bucket_date";

        return jdbcTemplate.query(sql, query.params, (rs, rowNum) -> {
            Date bucketDate = rs.getDate("bucket_date");
            return new DailyAlertCount(bucketDate.toLocalDate(), rs.getLong("alert_count"));
        });
    }

    public Map<String, Long> countByStatus(LocalDateTime from,
                                           LocalDateTime to,
                                           String severity) {
        return countByCategory("status", from, to, severity);
    }

    public Map<String, Long> countBySeverity(LocalDateTime from,
                                             LocalDateTime to,
                                             String severity) {
        return countByCategory("severity", from, to, severity);
    }

    public long countTransactions(LocalDateTime from, LocalDateTime to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transaction WHERE created_at >= :from AND created_at < :to",
                params,
                Long.class
        );
        return count == null ? 0L : count;
    }

    public List<DailyAlertCount> countTransactionsByDay(LocalDateTime from, LocalDateTime to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);
        String sql = "SELECT DATE(created_at) AS bucket_date, COUNT(*) AS transaction_count "
                + "FROM transaction "
                + "WHERE created_at >= :from AND created_at < :to "
                + "GROUP BY DATE(created_at) ORDER BY bucket_date";
        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new DailyAlertCount(rs.getDate("bucket_date").toLocalDate(),
                        rs.getLong("transaction_count")));
    }

    public List<DailyDuration> averageAcknowledgeByDay(LocalDateTime from,
                                                      LocalDateTime to,
                                                      String severity) {
        return averageDurationByDay("ack_at", from, to, severity, false);
    }

    public List<DailyDuration> averageResolutionByDay(LocalDateTime from,
                                                     LocalDateTime to,
                                                     String severity) {
        return averageDurationByDay("resolved_at", from, to, severity, true);
    }

    private List<DailyDuration> averageDurationByDay(String eventColumn,
                                                    LocalDateTime from,
                                                    LocalDateTime to,
                                                    String severity,
                                                    boolean terminalOnly) {
        MetricQuery query = buildRangeQuery(eventColumn, from, to, severity);
        String sql = "SELECT DATE(" + eventColumn + ") AS bucket_date, "
                + "AVG(TIMESTAMPDIFF(MICROSECOND, created_at, " + eventColumn + ") / 1000000.0) "
                + "AS average_seconds "
                + "FROM alert "
                + query.whereSql
                + " AND " + eventColumn + " IS NOT NULL"
                + " AND " + eventColumn + " >= created_at"
                + (terminalOnly ? " AND status IN ('CLOSED', 'DISMISSED')" : "")
                + " GROUP BY DATE(" + eventColumn + ")"
                + " ORDER BY bucket_date";
        return jdbcTemplate.query(sql, query.params, (rs, rowNum) ->
                new DailyDuration(
                        rs.getDate("bucket_date").toLocalDate(),
                        rs.getObject("average_seconds") == null ? null : rs.getDouble("average_seconds")
                ));
    }

    public SlaAggregate getSlaAggregate(LocalDateTime from,
                                        LocalDateTime to,
                                        String severity) {
        MetricQuery query = buildRangeQuery("created_at", from, to, severity);
        String sql = "SELECT COUNT(*) AS total_count, "
                + "COALESCE(SUM(CASE WHEN sla_breached = TRUE THEN 1 ELSE 0 END), 0) AS breached_count "
                + "FROM alert " + query.whereSql;
        return jdbcTemplate.queryForObject(sql, query.params, (rs, rowNum) ->
                new SlaAggregate(rs.getLong("total_count"), rs.getLong("breached_count")));
    }

    public List<DailySlaCount> getDailySlaCounts(LocalDateTime from,
                                                LocalDateTime to,
                                                String severity) {
        MetricQuery query = buildRangeQuery("created_at", from, to, severity);
        String sql = "SELECT DATE(created_at) AS bucket_date, COUNT(*) AS total_count, "
                + "COALESCE(SUM(CASE WHEN sla_breached = TRUE THEN 1 ELSE 0 END), 0) AS breached_count "
                + "FROM alert " + query.whereSql
                + " GROUP BY DATE(created_at) ORDER BY bucket_date";
        return jdbcTemplate.query(sql, query.params, (rs, rowNum) ->
                new DailySlaCount(
                        rs.getDate("bucket_date").toLocalDate(),
                        rs.getLong("total_count"),
                        rs.getLong("breached_count")
                ));
    }

    public ResolutionOutcomeAggregate getResolutionOutcomes(LocalDateTime from,
                                                            LocalDateTime to,
                                                            String severity) {
        MetricQuery query = buildRangeQuery("resolved_at", from, to, severity);
        String sql = "SELECT COUNT(*) AS resolved_count, "
                + "COALESCE(SUM(CASE WHEN status = 'DISMISSED' THEN 1 ELSE 0 END), 0) AS dismissed_count "
                + "FROM alert " + query.whereSql
                + " AND status IN ('CLOSED', 'DISMISSED')"
                + " AND resolved_at IS NOT NULL";
        return jdbcTemplate.queryForObject(sql, query.params, (rs, rowNum) ->
                new ResolutionOutcomeAggregate(
                        rs.getLong("resolved_count"),
                        rs.getLong("dismissed_count")
                ));
    }

    public List<RuleAlertAggregate> countAlertsByRule(LocalDateTime from,
                                                     LocalDateTime to,
                                                     String severity) {
        MetricQuery query = buildRangeQuery("a.created_at", "a.severity", from, to, severity);
        String sql = "SELECT a.rule_id, r.rule_name, COUNT(*) AS alert_count "
                + "FROM alert a JOIN monitoring_rule r ON r.id = a.rule_id "
                + query.whereSql
                + " GROUP BY a.rule_id, r.rule_name"
                + " ORDER BY alert_count DESC, a.rule_id";
        return jdbcTemplate.query(sql, query.params, (rs, rowNum) ->
                new RuleAlertAggregate(
                        rs.getLong("rule_id"),
                        rs.getString("rule_name"),
                        rs.getLong("alert_count")
                ));
    }

    private Map<String, Long> countByCategory(String category,
                                              LocalDateTime from,
                                              LocalDateTime to,
                                              String severity) {
        MetricQuery query = buildRangeQuery("created_at", from, to, severity);
        String sql = "SELECT " + category + " AS category, COUNT(*) AS alert_count "
                + "FROM alert "
                + query.whereSql
                + " GROUP BY " + category;

        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.query(sql, query.params, rs -> {
            counts.put(rs.getString("category"), rs.getLong("alert_count"));
        });
        return counts;
    }

    private MetricQuery buildRangeQuery(String dateColumn,
                                        LocalDateTime from,
                                        LocalDateTime to,
                                        String severity) {
        return buildRangeQuery(dateColumn, "severity", from, to, severity);
    }

    private MetricQuery buildRangeQuery(String dateColumn,
                                        String severityColumn,
                                        LocalDateTime from,
                                        LocalDateTime to,
                                        String severity) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (from != null) {
            where.append(" AND ").append(dateColumn).append(" >= :from");
            params.addValue("from", from);
        }
        if (to != null) {
            where.append(" AND ").append(dateColumn).append(" < :to");
            params.addValue("to", to);
        }
        if (severity != null) {
            where.append(" AND ").append(severityColumn).append(" = :severity");
            params.addValue("severity", severity);
        }

        return new MetricQuery(where.toString(), params);
    }

    private record MetricQuery(String whereSql, MapSqlParameterSource params) {
    }

    public record AverageResolutionAggregate(long resolvedAlertCount, Double averageResolutionSeconds) {
    }

    public record DailyAlertCount(LocalDate date, long count) {
    }

    public record DurationAggregate(long alertCount, Double averageSeconds) {
    }

    public record DailyDuration(LocalDate date, Double averageSeconds) {
    }

    public record SlaAggregate(long totalAlerts, long breachedAlerts) {
    }

    public record DailySlaCount(LocalDate date, long totalAlerts, long breachedAlerts) {
    }

    public record ResolutionOutcomeAggregate(long resolvedAlerts, long dismissedAlerts) {
    }

    public record RuleAlertAggregate(Long ruleId, String ruleName, long count) {
    }
}
