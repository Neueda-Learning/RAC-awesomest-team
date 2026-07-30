# Alert Dedup Transaction Traceability (2026-07-30)

## Purpose

Alert deduplication keeps one active alert for repeated triggers from the same
rule and account within the configured window. This change preserves every
transaction represented by that alert instead of retaining only the latest
transaction ID.

## Data Model

`alert.transaction_id` remains the latest triggering transaction for backward
compatibility.

The new `alert_transaction_link` table is the complete one-to-many audit:

| Column | Meaning |
|---|---|
| `alert_id` | The deduplicated alert |
| `transaction_id` | A transaction that triggered that alert |
| `triggered_at` | The time this transaction triggered the alert |

The primary key `(alert_id, transaction_id)` prevents duplicate links. Deleting
an alert cascades to its links. Transactions remain protected by a foreign key.

Alert creation/update and link insertion run inside the same Spring transaction.
A link failure therefore rolls back the alert change as well.

## Write Flow

For every triggered rule:

1. Find the latest active alert for the same rule and account within the dedup
   window.
2. If none exists, create an `OPEN` alert and link the triggering transaction.
3. If one exists, increment `dedup_count`, update `last_triggered_at` and the
   compatibility `transaction_id`, then append the new transaction link.

## Read API

`GET /alerts/{alertId}/transactions`

The response is ordered by latest alert trigger first:

```json
[
  {
    "transactionId": 20,
    "accountId": "ACC-009",
    "payeeId": "PAY-001",
    "amount": 20000.00,
    "currency": "USD",
    "transactionType": "TRANSFER_OUT",
    "description": "repeat trigger",
    "transactionCreatedAt": "2026-07-30T10:55:00",
    "alertTriggeredAt": "2026-07-30T11:00:00"
  }
]
```

A missing alert returns `404`.

## Existing Frontend Changes

Only `alerts.html` was changed:

- Pending and Resolved tables show `Occurrences`.
- Both tables show `Last Triggered`.
- `Transaction` is labelled `Latest Tx` to clarify compatibility semantics.
- Clicking an alert row opens a wide modal containing all linked transactions.
- The modal includes transaction/account/payee, amount/currency, type, original
  transaction time, alert-trigger time, and description.

Buttons and selection checkboxes stop row-click propagation, so lifecycle and
batch actions continue to work without unexpectedly opening the modal.

## Migration Boundary

Before this table existed, an alert merge overwrote `alert.transaction_id`.
Therefore schema initialization can backfill only the latest transaction still
stored on each existing alert. Earlier overwritten transaction IDs cannot be
recovered reliably.

The modal compares `dedupCount` with available links. If legacy links are
missing, it explicitly reports how many older merges cannot be reconstructed.
Every trigger processed after this migration is fully traceable.

## Verification

Automated coverage verifies:

- New alerts write their first transaction link.
- Dedup merges append another transaction link.
- Occurrence count and latest transaction fields still update.
- The related-transactions service rejects missing alerts.
- The controller returns linked transaction details.
- The existing HTML contains both new columns and the transaction modal.

Manual SQL:

```sql
SELECT
    a.id,
    a.dedup_count,
    COUNT(atl.transaction_id) AS linked_transactions,
    MAX(atl.triggered_at) AS latest_linked_trigger
FROM alert a
LEFT JOIN alert_transaction_link atl ON atl.alert_id = a.id
GROUP BY a.id, a.dedup_count
ORDER BY a.id DESC;
```

For alerts created entirely after this migration, `dedup_count` and
`linked_transactions` should match.

Validation completed:

- Full Maven suite passed (`51/51`).
- Inline JavaScript syntax validation passed.
- Application startup successfully applied the schema against MySQL.
- A live `GET /alerts/{id}/transactions` request returned the backfilled
  transaction for an existing alert.
