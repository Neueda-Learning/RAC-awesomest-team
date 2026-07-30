# HIGH-Severity Alert Email Notification (2026-07-30)

## 1. Behavior

The backend sends an email when a new `HIGH` alert is created.

- Email is emitted only after the alert transaction commits.
- `MEDIUM` and `LOW` alerts do not send email.
- A deduplicated trigger updates its existing alert but does not send another
  email.
- Email is disabled by default.
- SMTP failures never roll back or prevent alert creation.
- Failed deliveries are retained and retried on a fixed delay.

This implementation uses Spring's mail support and scheduling:

- https://docs.spring.io/spring-boot/reference/io/email.html
- https://docs.spring.io/spring-framework/reference/integration/scheduling.html

## 2. Runtime Configuration

Open `http://localhost:8080/alerts.html`, then select **Email Settings**.

The following non-secret values are stored in `alert_email_settings` and apply
without restarting:

- Enabled/disabled
- Sender and single administrator recipient
- SMTP host, port, and username
- SMTP authentication and STARTTLS switches
- Maximum attempts and retry delay

The browser shows only whether `SMTP_PASSWORD` is configured. The password is
not part of the GET/PUT API and is never stored in the database.

The first GET uses the existing environment/application defaults if no database
row has been saved. After the first Save Settings action, the database values
become authoritative for all non-secret fields.

Connection, read, and write timeouts are fixed at 5, 3, and 5 seconds so an
unavailable SMTP server cannot block indefinitely.

## 3. Password Setup

Prefer an application-specific password or SMTP token supplied by the mailbox
provider, not the normal interactive mailbox password.

Set the password in the same PowerShell process that starts the backend:

```powershell
$env:SMTP_PASSWORD = "replace-with-provider-app-password"

Set-Location backend
mvn spring-boot:run
```

The password is captured when the backend starts. Restart the backend after
changing it, then reload Email Settings and confirm the status is `Configured`.

A plain `.env` file is not automatically loaded by this Maven/Spring Boot
project. `.env` and `.env.*` are gitignored, but the file has no effect unless
Docker Compose or a startup script explicitly loads it into the backend process
environment. Do not commit a real password even if a deployment tool uses an
env file.

Provider examples:

- Google app passwords:
  https://support.google.com/accounts/answer/185833
- Microsoft app passwords:
  https://support.microsoft.com/en-us/accounts-billing/manage/how-to-get-and-use-app-passwords
- Apple app-specific passwords:
  https://support.apple.com/102654

## 4. Delivery Record

Every enabled HIGH-alert notification uses `alert_email_notification`.

| Status | Meaning |
|---|---|
| `PENDING` | Queued or currently being attempted |
| `SENT` | SMTP send returned successfully |
| `FAILED` | Latest attempt failed and may be retried |

The table records:

- Alert and recipient
- Attempt count
- Last attempt time
- Successful send time
- Latest error message
- Creation/update timestamps

One alert has at most one notification row. This prevents the alert-created
event from producing multiple independent email records.

## 5. Retry Rules

The scheduler scans every five seconds and processes up to 100 records whose
configured retry delay has elapsed.

- Both `PENDING` and `FAILED` records are recoverable after restart.
- A delivery is skipped once `attempt_count` reaches the configured maximum.
- Increasing Maximum Attempts in Email Settings allows previously exhausted
  failures to become retryable again.
- If recipient configuration is corrected, the next attempt refreshes the
  stored recipient before sending.

## 6. Email Content

The plain-text message includes:

- Alert ID, status, severity, and account
- Rule ID and rule name
- Alert creation time
- Acknowledge and resolution SLA deadlines
- Latest triggering transaction ID
- Payee, amount/currency, type, description, and transaction time

## 7. How to Verify

Create or enable a rule with severity `HIGH`, then submit a transaction that
triggers a new alert. Check the administrator mailbox and query:

```sql
SELECT
    id,
    alert_id,
    recipient,
    status,
    attempt_count,
    last_attempt_at,
    sent_at,
    error_message,
    created_at,
    updated_at
FROM alert_email_notification
ORDER BY id DESC;
```

Expected success:

- The alert exists normally.
- A notification row has `status = 'SENT'`.
- `attempt_count = 1`.
- `sent_at` is populated.

Expected SMTP failure:

- The alert still exists.
- A notification row has `status = 'FAILED'`.
- `error_message` contains the SMTP/configuration failure.
- `attempt_count` increases on later retry scans until the maximum.

To confirm dedup behavior, trigger the same HIGH rule for the same account again
inside its dedup window. `dedup_count` increases, a new transaction link is
stored, but no second notification row or email is created.

## 8. Security Notes

- Never commit `SMTP_PASSWORD`.
- Prefer an application-specific password or scoped SMTP credential.
- Restrict the sender account to mail delivery only.
- Use TLS for external SMTP providers.
- The settings API and database contain no password field.
- The project is currently single-admin mode without authentication. Protect
  `/admin/**` at the network or application-security layer before production
  exposure.

## 9. Personal Mailboxes and Domains

- A private mailbox can be used as sender or recipient if its provider allows
  authenticated SMTP access.
- This application applies normal email syntax validation but does not restrict
  Gmail, Outlook, iCloud, company, or other domains.
- The provider can still restrict the sender to the authenticated mailbox or a
  verified alias/domain.
- The recipient can normally be on another domain, subject to provider
  anti-spam, organization, and sending-limit rules.
- Sender and recipient may be the same mailbox.

Use **Send Test Email** after saving settings. It saves the current form first,
then sends a message using the same runtime SMTP path as alert delivery.

The built-in Spring mail health indicator is disabled because it can only see
static `spring.mail` properties, not the runtime database settings. The test
operation is the authoritative connectivity check for the effective settings.

## 10. Validation Record

- Full backend test suite passed: `65/65`.
- Email tests cover successful delivery, persisted failures, missing
  configuration, disabled mode, and retry-to-success.
- Rule-engine tests cover the HIGH-alert post-commit event and confirm that
  dedup merges do not request a second email.
- Settings tests cover validation, persistence, test delivery, and password
  non-disclosure in API responses.
- Frontend resource tests confirm the settings UI and absence of a password
  input.
- Startup validation created the settings table, returned health `UP`, and
  confirmed that the live GET response contains `passwordConfigured=false`
  without any password property.

## 11. API Contract

### Read effective settings

```http
GET /admin/email-settings
```

The response contains `passwordConfigured`, but no password value.

### Update non-secret settings

```http
PUT /admin/email-settings
Content-Type: application/json

{
  "enabled": true,
  "fromAddress": "sender@example.com",
  "toAddress": "admin@example.com",
  "smtpHost": "smtp.example.com",
  "smtpPort": 587,
  "smtpUsername": "sender@example.com",
  "smtpAuth": true,
  "starttlsEnabled": true,
  "starttlsRequired": true,
  "maxAttempts": 3,
  "retryDelayMs": 60000
}
```

### Send a test message

```http
POST /admin/email-settings/test
```
