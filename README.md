# RAC Awesomest Team - Transaction Monitoring System

## Overview
This project is a Spring Boot based transaction monitoring system with rule evaluation and alert lifecycle management.

When a transaction is created, the system evaluates active monitoring rules and automatically generates alerts for matched conditions.

## Current Features

### Backend Modules
- Transaction management (`/transactions`)
- Rule management (`/rules`)
- Alert management (`/alerts`)
- Rule engine with automatic alert generation

### Implemented Rule Types
- `AMOUNT_THRESHOLD`: triggers when a single transaction exceeds a threshold
- `VELOCITY`: triggers when transaction count exceeds limit in a time window
- `NEW_PAYEE`: triggers on first transfer to a payee from an account
- `DAILY_LIMIT`: triggers when daily total exceeds a threshold

### Alert Lifecycle
- `OPEN -> ACKNOWLEDGED`
- `ACKNOWLEDGED -> INVESTIGATING`
- `ACKNOWLEDGED/INVESTIGATING -> CLOSED`
- `ACKNOWLEDGED/INVESTIGATING -> DISMISSED`

### Test Data Generation
- Built-in API to generate mock transactions
- Configurable amount range and `createdAt` distribution
- Supports fixed step intervals or ranged distribution to avoid identical timestamps

### Unit Tests
- Service-level unit tests for Transaction, Rule, and Alert modules
- Mockito + JUnit 5 based tests

## Tech Stack
- Java 17
- Spring Boot 3.3.5
- Spring Data JDBC
- MySQL
- Maven

## Project Structure
```text
RAC-awesomest-team/
|- backend/
|  |- src/main/java/com/example/monitoring/
|  |  |- transaction/
|  |  |- rule/
|  |  |- alert/
|  |  |- common/
|  |- src/main/resources/
|  |  |- application.properties
|  |  |- schema.sql
|  |  |- static/
|  |     |- transactions.html
|  |     |- rule_engine.html
|  |     |- alerts.html
|  |- src/test/java/com/example/monitoring/
|- documents/
|- frontend/ (reserved for future standalone frontend)
```

## Prerequisites
- JDK 17
- Maven 3.9+
- MySQL 8+

## Database Setup
Run the following SQL in MySQL:

```sql
CREATE DATABASE IF NOT EXISTS transaction_monitoring CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY 'apppass';
GRANT ALL PRIVILEGES ON transaction_monitoring.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
```

Default connection settings are in `backend/src/main/resources/application.properties`:
- database: `transaction_monitoring`
- username: `appuser`
- password: `apppass`

## Run the Application

From the `backend` directory:

```powershell
Set-Location "C:\Users\Administrator\Desktop\RAC-awesomest-team\backend"
mvn spring-boot:run
```

By default, the backend starts at `http://localhost:8080`.

## Static Debug Pages
After backend startup, you can open:
- `http://localhost:8080/transactions.html`
- `http://localhost:8080/rule_engine.html`
- `http://localhost:8080/alerts.html`

## API Overview

### Transactions
- `POST /transactions` - create a transaction
- `GET /transactions` - list all transactions
- `GET /transactions/{id}` - get transaction by id
- `GET /transactions?accountId=ACC-001` - query by account
- `GET /transactions/search?keyword=wire` - search by description
- `GET /transactions/filter?minAmount=100&maxAmount=1000&from=...&to=...` - filter by amount/time
- `POST /transactions/generate` - generate mock transactions

### Rules
- `GET /rules`
- `GET /rules/{id}`
- `POST /rules`
- `PUT /rules/{id}`
- `DELETE /rules/{id}`

### Alerts
- `GET /alerts`
- `GET /alerts?status=OPEN`
- `GET /alerts/query` - server-side filtering, sorting, and pagination
- `GET /alerts/{id}`
- `GET /alerts/{id}/history`
- `GET /alerts/metrics/average-resolution?from=...&to=...&severity=HIGH`
- `GET /alerts/metrics/trend?days=7&severity=HIGH`
- `GET /alerts/metrics/summary?from=...&to=...&severity=HIGH`
- `GET /alerts/metrics/dashboard?days=30&severity=HIGH`
- `PATCH /alerts/{id}/acknowledge`
- `PATCH /alerts/{id}/investigate`
- `PATCH /alerts/{id}/close`
- `PATCH /alerts/{id}/dismiss`

## Mock Data Generation

### Basic
```powershell
curl -X POST "http://localhost:8080/transactions/generate?count=100"
```

### Controlled amount range and fixed timestamp step
```powershell
curl -X POST "http://localhost:8080/transactions/generate?count=20&minAmount=100&maxAmount=500&startAt=2026-07-28T10:00:00&stepSeconds=120"
```

### Controlled amount range and timestamp range
```powershell
curl -X POST "http://localhost:8080/transactions/generate?count=20&minAmount=100&maxAmount=500&startAt=2026-07-28T10:00:00&endAt=2026-07-28T11:00:00"
```

Supported query params:
- `count` (default `100`)
- `minAmount`
- `maxAmount`
- `startAt` (ISO-8601 datetime)
- `endAt` (ISO-8601 datetime)
- `stepSeconds`

## Run Unit Tests

From the `backend` directory:

```powershell
Set-Location "C:\Users\Administrator\Desktop\RAC-awesomest-team\backend"
mvn test
```

Current test coverage includes:
- `TransactionServiceTest`
- `RuleServiceTest`
- `AlertServiceTest`

## Example Rule Validation Scenarios

### 1) Amount Threshold
Send a transaction with amount greater than `10000`:

```json
{
  "accountId": "ACC-001",
  "payeeId": "PAYEE-BANK",
  "amount": 15000,
  "transactionType": "DEBIT",
  "description": "High value wire transfer"
}
```

Expected: transaction is saved and an alert is generated.

### 2) New Payee
Send a first transfer to a new payee:

```json
{
  "accountId": "ACC-002",
  "payeeId": "PAYEE-UNKNOWN-NEW",
  "amount": 500,
  "transactionType": "DEBIT",
  "description": "Payment to new vendor"
}
```

Expected: transaction is saved and a new payee alert is generated.

### 3) Velocity
Send more than 5 transactions within the configured time window for the same account.

Expected: velocity alerts appear after threshold crossing.

### 4) Daily Limit
Send multiple large transactions in one day for the same account so the daily total exceeds configured limit.

Expected: daily limit alert is generated.

## Troubleshooting

### Port still occupied after stopping app
- Use IDE `Stop` on the actual running process (not only close the tool window)
- If started via terminal, stop with `Ctrl + C`
- Graceful shutdown is enabled in `application.properties`

### Java version mismatch
If you see `release version XX not supported`, verify Maven runtime:

```powershell
mvn -v
java -version
```

Ensure Maven uses JDK 17 for this project.

## Additional Project Docs
- `documents/unittest_and_db_data_generation.md`
- `documents/port_release_troubleshooting.md`
- `documents/7.27项目重构.md`
- `documents/alert_metrics_2026-07-30.md`
- `documents/dashboard_modification_and_plan_2026-07-30.md`

## Contribution Notes
- Do not push directly to `main`
- Work in feature branches and open Pull Requests
- Keep build outputs out of Git (`target/` is ignored)
