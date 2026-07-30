# Alert Metrics and Dashboard Integration (2026-07-30)

## Scope

This change completes Alert Improvement Plan sub-steps 3.1, 3.2, and 3.3:

- backend-calculated average resolution time;
- UTC-aligned 7-day and 30-day alert trends;
- dashboard cards and charts backed by compact metric APIs.

All metric range bounds use UTC. `from` is inclusive and `to` is exclusive.

## Average Resolution API

`GET /alerts/metrics/average-resolution`

Optional query parameters:

- `from`: ISO-8601 UTC instant, for example `2026-07-01T00:00:00Z`;
- `to`: ISO-8601 UTC instant, for example `2026-08-01T00:00:00Z`;
- `severity`: `HIGH`, `MEDIUM`, or `LOW`.

The reporting range is applied to `resolved_at`. Only `CLOSED` and `DISMISSED`
alerts with a valid `resolved_at >= created_at` are included.

Example response:

```json
{
  "from": "2026-07-01T00:00:00Z",
  "to": "2026-08-01T00:00:00Z",
  "severity": "HIGH",
  "resolvedAlertCount": 4,
  "averageResolutionSeconds": 92.5
}
```

When no matching alert has been resolved, `resolvedAlertCount` is `0` and
`averageResolutionSeconds` is `null`.

## Recent Trend API

`GET /alerts/metrics/trend?days=7&severity=HIGH`

Query parameters:

- `days`: optional, defaults to `7`; accepted values are `7` and `30`;
- `severity`: optional, one of `HIGH`, `MEDIUM`, or `LOW`.

The endpoint counts alerts by `created_at` and returns one UTC bucket for every
day, including zero-count days.

Example response:

```json
{
  "days": 7,
  "from": "2026-07-24T00:00:00Z",
  "to": "2026-07-31T00:00:00Z",
  "severity": null,
  "timeZone": "UTC",
  "buckets": [
    { "date": "2026-07-24", "count": 2 },
    { "date": "2026-07-25", "count": 0 }
  ]
}
```

## Dashboard Summary API

`GET /alerts/metrics/summary`

This compact supporting endpoint prevents dashboard status/severity cards and
charts from downloading every alert row. It accepts the same optional `from`,
`to`, and `severity` parameters as the average-resolution endpoint. Its range
is applied to `created_at`.

Example response:

```json
{
  "from": "2026-07-01T00:00:00Z",
  "to": "2026-08-01T00:00:00Z",
  "severity": null,
  "totalAlerts": 12,
  "statusCounts": {
    "OPEN": 3,
    "ACKNOWLEDGED": 2,
    "INVESTIGATING": 1,
    "CLOSED": 5,
    "DISMISSED": 1
  },
  "severityCounts": {
    "HIGH": 4,
    "MEDIUM": 5,
    "LOW": 3
  }
}
```

Missing categories are returned with a zero value so chart series remain stable.

## Frontend Behavior

- Dashboard startup requests the consolidated compact endpoint
  `GET /alerts/metrics/dashboard?days=7|30&severity=...`.
- Full `GET /alerts` loading is deferred until Pending Alerts or Resolved Alerts
  is opened.
- Dashboard operators can select a 7-day or 30-day window and an optional
  severity.
- KPI values and all charts use server-calculated aggregates.
- The dashboard additionally includes transaction trend, acknowledgement and
  resolution-time trend, SLA breach-rate trend, and alerts grouped by rule.

## Time-Zone Handling

The MySQL JDBC connection is configured with UTC as the connection/session time
zone. Trend boundaries and zero-fill logic are calculated with a UTC clock.
Idempotent `created_at` and `resolved_at` indexes support the reporting-range
queries in existing and new database environments.
