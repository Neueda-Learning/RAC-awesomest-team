-- Transaction Monitoring & Alerts Dashboard
-- Database schema for transaction monitoring and real-time alert system
-- Run once against transaction_monitoring database

-- 交易表
CREATE TABLE IF NOT EXISTS transaction (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    account_id      VARCHAR(50)   NOT NULL,
    payee_id        VARCHAR(50)   NOT NULL,
    amount          DECIMAL(15, 2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'USD',
    transaction_type VARCHAR(20)   NOT NULL,  -- DEBIT, CREDIT
    description     VARCHAR(255),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_created (account_id, created_at),
    INDEX idx_payee (payee_id)
);

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
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_rule FOREIGN KEY (rule_id) REFERENCES monitoring_rule (id),
    CONSTRAINT fk_alert_transaction FOREIGN KEY (transaction_id) REFERENCES transaction (id),
    INDEX idx_status (status),
    INDEX idx_account_created (account_id, created_at),
    INDEX idx_severity (severity)
);

-- 告警状态历史表
CREATE TABLE IF NOT EXISTS alert_status_history (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    alert_id        BIGINT        NOT NULL,
    old_status      VARCHAR(50),
    new_status      VARCHAR(50)   NOT NULL,
    notes           TEXT,
    changed_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_alert FOREIGN KEY (alert_id) REFERENCES alert (id)
);


