# Alert Bulk Operations and Filtered Export (2026-07-30)

## Scope

This delivery completes Alert Improvement Plan phases 4.1, 4.2, and 4.3.
It adds backend batch lifecycle operations, reproducible filtered CSV export,
and operator controls in the existing `alerts.html`. No new frontend file was
created and no database migration is required.

## Bulk Status API

`POST /alerts/bulk/status`

Request:

```json
{
  "ids": [101, 102],
  "action": "ACKNOWLEDGE",
  "notes": "Reviewed in the morning queue"
}
```

Rules:

- `ids` must contain 1 to 100 unique positive IDs.
- `action` is one of `ACKNOWLEDGE`, `INVESTIGATE`, `CLOSE`, or `DISMISS`.
- `notes` is optional and limited to 1,000 characters.
- Each item uses the existing lifecycle rules and writes the normal status
  history entry when successful.
- A missing alert or invalid transition is reported for that item; processing
  continues for the remaining IDs.

Response:

```json
{
  "requestedCount": 2,
  "successCount": 1,
  "failureCount": 1,
  "results": [
    {
      "id": 101,
      "success": true,
      "status": "ACKNOWLEDGED",
      "error": null
    },
    {
      "id": 102,
      "success": false,
      "status": null,
      "error": "Can only acknowledge OPEN alerts. Current status: CLOSED"
    }
  ]
}
```

## Filtered CSV API

`GET /alerts/export?format=csv`

Supported filters match the alert query contract:

- `status`
- `statusGroup=ACTIVE|RESOLVED`
- `severity`
- `accountId`
- `ruleId`
- `slaBreached`
- `from` and `to`
- `sortBy` and `sortDir`

An exact `status` takes precedence over `statusGroup`. The result order is
stable because alert ID is used as a secondary sort. Severity sorting uses the
business order `HIGH`, `MEDIUM`, `LOW`.

The export contains a UTF-8 BOM for spreadsheet compatibility and includes:

`id`, `ruleId`, `transactionId`, `accountId`, `severity`, `status`,
`dedupCount`, `lastTriggeredAt`, `slaBreached`, `ackAt`, `resolvedAt`,
`ackDueAt`, `resolveDueAt`, `createdAt`, and `updatedAt`.

CSV values containing commas, quotes, or line breaks are quoted and escaped.
Exports above 5,000 rows return `400 Bad Request`; the operator must refine the
filters.

## Existing-Page UI Changes

The Pending page now has:

- Per-row selection checkboxes.
- Select-all for the currently visible page.
- A selected-count indicator.
- A responsive toolbar for all four lifecycle actions.
- Per-item failure details after partial batch completion.
- Filtered CSV export.

The Resolved page has filtered CSV export. It does not expose batch lifecycle
buttons because `CLOSED` and `DISMISSED` are terminal states.

Selections outside the active filter are removed. Successful batch items are
unselected, while failures remain selected so an operator can review or retry
them.

## Verification

Coverage includes:

- Partial batch success and history recording.
- Duplicate ID rejection before writes.
- Controller request validation and per-item JSON.
- Filter normalization, CSV escaping, and the 5,000-row ceiling.
- Download response headers and UTF-8 bytes.
- Frontend resource wiring for selection, batch actions, and export.

The full Maven suite passed (`44/44`) and the inline JavaScript passed a Node.js
syntax check.
