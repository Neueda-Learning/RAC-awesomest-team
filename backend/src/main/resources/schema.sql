-- Transaction Monitoring & Alerts Dashboard
-- Database schema for transaction monitoring and real-time alert system
-- Run once against transaction_monitoring database

-- 交易表
CREATE TABLE IF NOT EXISTS transaction (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    account_id      VARCHAR(50)   NOT NULL,
    payee_id        VARCHAR(50),
    amount          DECIMAL(15, 2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'USD',
    transaction_type VARCHAR(20)   NOT NULL,  -- SALARY, REFUND, TRANSFER_OUT, DEPOSIT, WITHDRAWAL
    description     VARCHAR(255),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_created (account_id, created_at),
    INDEX idx_transaction_created_at (created_at),
    INDEX idx_payee (payee_id)
);

-- 兼容已存在的旧表结构：允许 payee_id 为空（用于存款/取款）
ALTER TABLE transaction MODIFY COLUMN payee_id VARCHAR(50) NULL;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction' AND INDEX_NAME = 'idx_transaction_created_at');
SET @idx_sql = IF(@idx_exists = 0, 'CREATE INDEX idx_transaction_created_at ON transaction (created_at)', 'SELECT 1');
PREPARE add_transaction_idx_stmt FROM @idx_sql;
EXECUTE add_transaction_idx_stmt;
DEALLOCATE PREPARE add_transaction_idx_stmt;

-- 监控规则表
CREATE TABLE IF NOT EXISTS monitoring_rule (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    rule_name       VARCHAR(100)  NOT NULL UNIQUE,
    rule_type       VARCHAR(50)   NOT NULL,  -- AMOUNT_THRESHOLD, VELOCITY, NEW_PAYEE, DAILY_LIMIT
    severity        VARCHAR(20)   NOT NULL,  -- HIGH, MEDIUM, LOW
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    threshold_value DECIMAL(15, 2),
    time_window_minutes INT,
    max_count       INT,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 告警表
CREATE TABLE IF NOT EXISTS alert (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    rule_id         BIGINT        NOT NULL,
    transaction_id  BIGINT        NOT NULL,
    account_id      VARCHAR(50)   NOT NULL,
    severity        VARCHAR(20)   NOT NULL,  -- HIGH, MEDIUM, LOW
    status          VARCHAR(50)   NOT NULL,  -- OPEN, ACKNOWLEDGED, INVESTIGATING, CLOSED, DISMISSED
    dedup_count     INT           NOT NULL DEFAULT 1,
    last_triggered_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ack_at          TIMESTAMP     NULL,
    resolved_at     TIMESTAMP     NULL,
    ack_due_at      TIMESTAMP     NULL,
    resolve_due_at  TIMESTAMP     NULL,
    sla_breached    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_rule FOREIGN KEY (rule_id) REFERENCES monitoring_rule (id),
    CONSTRAINT fk_alert_transaction FOREIGN KEY (transaction_id) REFERENCES transaction (id),
    INDEX idx_status (status),
    INDEX idx_account_created (account_id, created_at),
    INDEX idx_severity (severity),
    INDEX idx_status_created (status, created_at),
    INDEX idx_severity_created (severity, created_at),
    INDEX idx_rule_created (rule_id, created_at),
    INDEX idx_sla_breached (sla_breached, status),
    INDEX idx_created_at (created_at),
    INDEX idx_ack_at (ack_at),
    INDEX idx_resolved_at (resolved_at)
);

-- 告警表兼容升级：补充去重与 SLA 字段（幂等）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND COLUMN_NAME = 'dedup_count');
SET @alter_sql = IF(@col_exists = 0, 'ALTER TABLE alert ADD COLUMN dedup_count INT NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE add_alert_col_stmt FROM @alter_sql;
EXECUTE add_alert_col_stmt;
DEALLOCATE PREPARE add_alert_col_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND COLUMN_NAME = 'last_triggered_at');
SET @alter_sql = IF(@col_exists = 0, 'ALTER TABLE alert ADD COLUMN last_triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE add_alert_col_stmt FROM @alter_sql;
EXECUTE add_alert_col_stmt;
DEALLOCATE PREPARE add_alert_col_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND COLUMN_NAME = 'ack_at');
SET @alter_sql = IF(@col_exists = 0, 'ALTER TABLE alert ADD COLUMN ack_at TIMESTAMP NULL', 'SELECT 1');
PREPARE add_alert_col_stmt FROM @alter_sql;
EXECUTE add_alert_col_stmt;
DEALLOCATE PREPARE add_alert_col_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND COLUMN_NAME = 'resolved_at');
SET @alter_sql = IF(@col_exists = 0, 'ALTER TABLE alert ADD COLUMN resolved_at TIMESTAMP NULL', 'SELECT 1');
PREPARE add_alert_col_stmt FROM @alter_sql;
EXECUTE add_alert_col_stmt;
DEALLOCATE PREPARE add_alert_col_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND COLUMN_NAME = 'ack_due_at');
SET @alter_sql = IF(@col_exists = 0, 'ALTER TABLE alert ADD COLUMN ack_due_at TIMESTAMP NULL', 'SELECT 1');
PREPARE add_alert_col_stmt FROM @alter_sql;
EXECUTE add_alert_col_stmt;
DEALLOCATE PREPARE add_alert_col_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND COLUMN_NAME = 'resolve_due_at');
SET @alter_sql = IF(@col_exists = 0, 'ALTER TABLE alert ADD COLUMN resolve_due_at TIMESTAMP NULL', 'SELECT 1');
PREPARE add_alert_col_stmt FROM @alter_sql;
EXECUTE add_alert_col_stmt;
DEALLOCATE PREPARE add_alert_col_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND COLUMN_NAME = 'sla_breached');
SET @alter_sql = IF(@col_exists = 0, 'ALTER TABLE alert ADD COLUMN sla_breached BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE add_alert_col_stmt FROM @alter_sql;
EXECUTE add_alert_col_stmt;
DEALLOCATE PREPARE add_alert_col_stmt;

-- 旧数据修复：初始化去重与 SLA 布尔字段
UPDATE alert SET dedup_count = 1 WHERE dedup_count IS NULL OR dedup_count < 1;
UPDATE alert SET last_triggered_at = created_at WHERE last_triggered_at IS NULL;
UPDATE alert SET sla_breached = FALSE WHERE sla_breached IS NULL;

-- 告警表索引兼容升级（幂等）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND INDEX_NAME = 'idx_status_created');
SET @idx_sql = IF(@idx_exists = 0, 'CREATE INDEX idx_status_created ON alert (status, created_at)', 'SELECT 1');
PREPARE add_alert_idx_stmt FROM @idx_sql;
EXECUTE add_alert_idx_stmt;
DEALLOCATE PREPARE add_alert_idx_stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND INDEX_NAME = 'idx_severity_created');
SET @idx_sql = IF(@idx_exists = 0, 'CREATE INDEX idx_severity_created ON alert (severity, created_at)', 'SELECT 1');
PREPARE add_alert_idx_stmt FROM @idx_sql;
EXECUTE add_alert_idx_stmt;
DEALLOCATE PREPARE add_alert_idx_stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND INDEX_NAME = 'idx_rule_created');
SET @idx_sql = IF(@idx_exists = 0, 'CREATE INDEX idx_rule_created ON alert (rule_id, created_at)', 'SELECT 1');
PREPARE add_alert_idx_stmt FROM @idx_sql;
EXECUTE add_alert_idx_stmt;
DEALLOCATE PREPARE add_alert_idx_stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND INDEX_NAME = 'idx_sla_breached');
SET @idx_sql = IF(@idx_exists = 0, 'CREATE INDEX idx_sla_breached ON alert (sla_breached, status)', 'SELECT 1');
PREPARE add_alert_idx_stmt FROM @idx_sql;
EXECUTE add_alert_idx_stmt;
DEALLOCATE PREPARE add_alert_idx_stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND INDEX_NAME = 'idx_created_at');
SET @idx_sql = IF(@idx_exists = 0, 'CREATE INDEX idx_created_at ON alert (created_at)', 'SELECT 1');
PREPARE add_alert_idx_stmt FROM @idx_sql;
EXECUTE add_alert_idx_stmt;
DEALLOCATE PREPARE add_alert_idx_stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND INDEX_NAME = 'idx_resolved_at');
SET @idx_sql = IF(@idx_exists = 0, 'CREATE INDEX idx_resolved_at ON alert (resolved_at)', 'SELECT 1');
PREPARE add_alert_idx_stmt FROM @idx_sql;
EXECUTE add_alert_idx_stmt;
DEALLOCATE PREPARE add_alert_idx_stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert' AND INDEX_NAME = 'idx_ack_at');
SET @idx_sql = IF(@idx_exists = 0, 'CREATE INDEX idx_ack_at ON alert (ack_at)', 'SELECT 1');
PREPARE add_alert_idx_stmt FROM @idx_sql;
EXECUTE add_alert_idx_stmt;
DEALLOCATE PREPARE add_alert_idx_stmt;

-- 告警状态历史表
-- Every transaction represented by an alert, including deduplicated triggers.
CREATE TABLE IF NOT EXISTS alert_transaction_link (
    alert_id        BIGINT      NOT NULL,
    transaction_id  BIGINT      NOT NULL,
    triggered_at    TIMESTAMP   NOT NULL,
    PRIMARY KEY (alert_id, transaction_id),
    CONSTRAINT fk_alert_transaction_link_alert
        FOREIGN KEY (alert_id) REFERENCES alert (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_transaction_link_transaction
        FOREIGN KEY (transaction_id) REFERENCES transaction (id),
    INDEX idx_alert_transaction_triggered (alert_id, triggered_at)
);

-- The legacy model retained only the latest transaction. Backfill that known
-- link; every future merge appends its transaction without overwriting history.
INSERT IGNORE INTO alert_transaction_link (alert_id, transaction_id, triggered_at)
SELECT id, transaction_id, COALESCE(last_triggered_at, created_at)
FROM alert;

-- Runtime-editable non-secret email settings. SMTP_PASSWORD stays in the
-- backend process environment and is never stored here.
CREATE TABLE IF NOT EXISTS alert_email_settings (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    singleton_key       TINYINT       NOT NULL DEFAULT 1,
    enabled             BOOLEAN       NOT NULL DEFAULT FALSE,
    from_address        VARCHAR(320)  NOT NULL,
    to_address          VARCHAR(320)  NOT NULL,
    smtp_host           VARCHAR(255)  NOT NULL,
    smtp_port           INT           NOT NULL,
    smtp_username       VARCHAR(320)  NOT NULL DEFAULT '',
    smtp_auth           BOOLEAN       NOT NULL DEFAULT TRUE,
    starttls_enabled    BOOLEAN       NOT NULL DEFAULT TRUE,
    starttls_required   BOOLEAN       NOT NULL DEFAULT TRUE,
    max_attempts        INT           NOT NULL DEFAULT 3,
    retry_delay_ms      BIGINT        NOT NULL DEFAULT 60000,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_settings_singleton UNIQUE (singleton_key)
);

SET @email_settings_singleton_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert_email_settings'
      AND COLUMN_NAME = 'singleton_key');
SET @email_settings_alter_sql = IF(
    @email_settings_singleton_col_exists = 0,
    'ALTER TABLE alert_email_settings ADD COLUMN singleton_key TINYINT NOT NULL DEFAULT 1 AFTER id',
    'SELECT 1');
PREPARE add_email_settings_singleton_col_stmt FROM @email_settings_alter_sql;
EXECUTE add_email_settings_singleton_col_stmt;
DEALLOCATE PREPARE add_email_settings_singleton_col_stmt;

SET @email_settings_singleton_idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alert_email_settings'
      AND INDEX_NAME = 'uk_email_settings_singleton');
SET @email_settings_index_sql = IF(
    @email_settings_singleton_idx_exists = 0,
    'CREATE UNIQUE INDEX uk_email_settings_singleton ON alert_email_settings (singleton_key)',
    'SELECT 1');
PREPARE add_email_settings_singleton_idx_stmt FROM @email_settings_index_sql;
EXECUTE add_email_settings_singleton_idx_stmt;
DEALLOCATE PREPARE add_email_settings_singleton_idx_stmt;

-- Delivery audit and retry state for HIGH-severity alert emails.
CREATE TABLE IF NOT EXISTS alert_email_notification (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    alert_id        BIGINT        NOT NULL,
    recipient       VARCHAR(320)  NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    attempt_count   INT           NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP     NULL,
    sent_at         TIMESTAMP     NULL,
    error_message   TEXT          NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_notification_alert
        FOREIGN KEY (alert_id) REFERENCES alert (id) ON DELETE CASCADE,
    CONSTRAINT uk_email_notification_alert UNIQUE (alert_id),
    INDEX idx_email_notification_retry (status, attempt_count, updated_at)
);

CREATE TABLE IF NOT EXISTS alert_status_history (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    alert_id        BIGINT        NOT NULL,
    old_status      VARCHAR(50),
    new_status      VARCHAR(50)   NOT NULL,
    notes           TEXT,
    changed_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_alert FOREIGN KEY (alert_id) REFERENCES alert (id)
);

-- 初始规则数据（仅在表为空时插入）
INSERT INTO monitoring_rule (rule_name, rule_type, severity, is_active, threshold_value, time_window_minutes, max_count)
SELECT 'High Value Transaction', 'AMOUNT_THRESHOLD', 'LOW', TRUE, 10000.00, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM monitoring_rule WHERE rule_name = 'High Value Transaction');

INSERT INTO monitoring_rule (rule_name, rule_type, severity, is_active, threshold_value, time_window_minutes, max_count)
SELECT 'Rapid Transactions', 'VELOCITY', 'MEDIUM', TRUE, NULL, 10, 5
WHERE NOT EXISTS (SELECT 1 FROM monitoring_rule WHERE rule_name = 'Rapid Transactions');

INSERT INTO monitoring_rule (rule_name, rule_type, severity, is_active, threshold_value, time_window_minutes, max_count)
SELECT 'New Payee', 'NEW_PAYEE', 'LOW', TRUE, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM monitoring_rule WHERE rule_name = 'New Payee');

INSERT INTO monitoring_rule (rule_name, rule_type, severity, is_active, threshold_value, time_window_minutes, max_count)
SELECT 'Daily Limit Exceeded', 'DAILY_LIMIT', 'HIGH', TRUE, 50000.00, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM monitoring_rule WHERE rule_name = 'Daily Limit Exceeded');

-- 统一默认告警等级（对已存在规则也生效）
UPDATE monitoring_rule SET severity = 'LOW' WHERE rule_type = 'AMOUNT_THRESHOLD';
UPDATE monitoring_rule SET severity = 'MEDIUM' WHERE rule_type = 'VELOCITY';
UPDATE monitoring_rule SET severity = 'LOW' WHERE rule_type = 'NEW_PAYEE';
UPDATE monitoring_rule SET severity = 'HIGH' WHERE rule_type = 'DAILY_LIMIT';

-- 复杂规则支持：给 monitoring_rule 加 logic_operator 列（AND/OR），幂等写法
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monitoring_rule' AND COLUMN_NAME = 'logic_operator');
SET @alter_sql = IF(@col_exists = 0, 'ALTER TABLE monitoring_rule ADD COLUMN logic_operator VARCHAR(3) DEFAULT NULL', 'SELECT 1');
PREPARE add_col_stmt FROM @alter_sql;
EXECUTE add_col_stmt;
DEALLOCATE PREPARE add_col_stmt;

-- 复杂规则的子条件表
CREATE TABLE IF NOT EXISTS rule_condition (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    rule_id             BIGINT        NOT NULL,
    condition_type      VARCHAR(50)   NOT NULL,
    threshold_value     DECIMAL(15, 2),
    time_window_minutes INT,
    max_count           INT,
    start_hour          INT,
    end_hour            INT,
    CONSTRAINT fk_condition_rule FOREIGN KEY (rule_id) REFERENCES monitoring_rule (id) ON DELETE CASCADE
);

