# Alert Frontend Iteration (2026-07-28)

## Scope
Enhanced `backend/src/main/resources/static/alerts.html` to improve dashboard visibility and alert operations.

## Implemented
1. Dashboard charts
   - Added status distribution pie chart
   - Added severity distribution bar chart

2. Processed vs unprocessed separation
   - Unprocessed section: `OPEN`
   - Processed section: non-`OPEN` statuses

3. Advanced multi-condition filtering
   - Status
   - Severity
   - Rule ID
   - Account ID
   - Time range (`From` / `To`)
   - Processed flag (`All` / `Unprocessed` / `Processed`)
   - Sort by created time and severity

4. Expandable transaction details per alert
   - Click an alert row to expand
   - Fetches transaction details from `GET /transactions/{id}`
   - Shows amount, currency, payee, type, description, and timestamp

## Notes
- This system is single-admin only; no identity/user features were introduced.
- Existing alert lifecycle constraints remain enforced by backend service logic.

