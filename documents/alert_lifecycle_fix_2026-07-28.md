# Alert Lifecycle Fix (2026-07-28)

## Change

Adjusted the close transition rule to match the required lifecycle:

`OPEN -> ACKNOWLEDGED -> INVESTIGATING -> CLOSED`

`DISMISSED` is still allowed from `ACKNOWLEDGED` and `INVESTIGATING`.

## What Was Fixed

Before:
- `ACKNOWLEDGED -> CLOSED` was allowed.

Now:
- `ACKNOWLEDGED -> CLOSED` is blocked.
- Only `INVESTIGATING -> CLOSED` is allowed.

## Updated Files

- `backend/src/main/java/com/example/monitoring/alert/service/AlertService.java`
- `backend/src/main/java/com/example/monitoring/alert/controller/AlertController.java`
- `backend/src/main/resources/static/alerts.html`
- `backend/src/test/java/com/example/monitoring/alert/AlertServiceTest.java`
- `documents/module_completion_status.md`

## Validation

- Added unit test to verify closing an `ACKNOWLEDGED` alert throws `IllegalStateException`.
- Existing test for `INVESTIGATING -> CLOSED` still passes.

