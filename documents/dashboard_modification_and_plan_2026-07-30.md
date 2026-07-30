# Dashboard Modification Record and Plan (2026-07-30)

## 1. Objective

This iteration improves the Alert Dashboard based on the advanced chart
recommendations in `transaction_monitoring.md`.

The frontend remains a single static page:

- modified: `backend/src/main/resources/static/alerts.html`;
- no new frontend HTML, JavaScript, CSS, or framework files were created.

## 2. Removed Dashboard Items

The following individual lifecycle KPI cards were removed:

- `OPEN`;
- `ACKNOWLEDGED`;
- `INVESTIGATING`;
- `CLOSED`;
- `DISMISSED`.

Lifecycle visibility is retained through the Status Distribution doughnut chart.
This reduces repeated information and leaves the top of the dashboard for
decision-oriented metrics.

## 3. Added KPI Cards

The Operational Snapshot now contains:

| KPI | Definition |
|---|---|
| Alerts Generated | Alert records created inside the selected UTC window |
| Transactions Recorded | All transactions created inside the window |
| Alert Trigger Rate | Alerts generated / transactions recorded |
| Average Acknowledge Time | Average `created_at -> ack_at`, grouped by acknowledgement completion |
| Average Resolution Time | Average `created_at -> resolved_at` for closed/dismissed alerts |
| SLA Breach Rate | SLA-breached alerts / alerts created |
| False Positive Rate | Dismissed alerts / all closed or dismissed alerts |

The acknowledgement and resolution cards also show their sample sizes.

The severity selector filters alert-based KPIs. Transaction counts remain
unfiltered because transactions do not have an alert severity.

## 4. Added and Retained Charts

### Activity Trends

- Transactions Over Time: daily transaction count.
- Alerts Over Time: daily alert count, with optional severity filtering.

### Response and SLA Performance

- Alert Response Times Trend:
  - average acknowledgement duration;
  - average resolution duration;
  - grouped by the UTC day on which each lifecycle event completed.
- SLA Breach Rate Trend:
  - daily breached count / daily alert count;
  - zero-filled UTC dates keep the line chart stable.

### Alert Composition

- Status Distribution: retained and displayed as a doughnut chart.
- Severity Distribution: retained as a bar chart.
- Alerts by Rule: alert count grouped by monitoring rule.

## 5. Layout Decisions

The dashboard is divided into four clearly labelled sections:

1. Operational Snapshot;
2. Activity Trends;
3. Response & SLA Performance;
4. Alert Composition.

Layout behavior:

- KPI cards use a responsive auto-fit grid.
- Activity charts use balanced side-by-side cards on wide screens.
- Response-time history receives more width than the SLA percentage chart.
- Composition charts automatically form three columns where space permits.
- At tablet/mobile widths, all chart groups collapse to one column.
- Chart subtitles clarify metric scope and avoid ambiguous interpretation.

## 6. Backend Contract

The page uses one compact endpoint:

`GET /alerts/metrics/dashboard?days=30&severity=HIGH`

Supported windows:

- `days=7`;
- `days=30` (default).

The response includes:

- UTC reporting boundaries;
- KPI values and sample counts;
- status and severity aggregates;
- zero-filled alert and transaction trends;
- response-time series;
- SLA rate series;
- alert counts by rule.

No full alert or transaction collection is loaded to render the dashboard.

## 7. Query Performance

The schema includes idempotent indexes for the dashboard reporting paths:

- `transaction(created_at)`;
- `alert(created_at)`;
- `alert(ack_at)`;
- `alert(resolved_at)`.

## 8. Known Metric Limitations

- `sla_breached` currently reflects the existing lifecycle transition logic.
  An active overdue alert is not marked breached until the application evaluates
  its SLA state.
- False-positive rate uses `DISMISSED` as the available false-positive proxy.
- Transaction trend shows counts only. Cross-currency volume should not be
  summed until amounts have a persisted normalized/base-currency value.
- The project assumes a single operator, so operator-comparison charts are out
  of scope.

## 9. Next Dashboard Plan

### P1

- Add scheduled SLA-state evaluation so active overdue alerts immediately affect
  SLA metrics.
- Add custom UTC date ranges in addition to the 7/30-day presets.
- Add previous-period comparison indicators to KPI cards.

### P2

- Persist normalized USD/base-currency amounts and add transaction volume trend.
- Add alert-to-transaction conversion by rule.
- Add top risky accounts/payees with safe result limits.

### P3

- Add automatic refresh or Server-Sent Events for near-real-time updates.
- Add CSV/report export using the same dashboard reporting window.
- Add query timing telemetry and caching if reporting volume grows.

## 10. Validation

- Focused dashboard metric/frontend tests passed (`12/12`).
- Full Maven test suite passed (`37/37`).
- Inline JavaScript syntax validation passed.
- Dashboard resource tests verify that the five removed lifecycle KPI card IDs
  are absent and all new chart canvases are present.
