mysql workbench所需操作：
运行这行代码来创建数据库：CREATE DATABASE IF NOT EXISTS transaction_monitoring CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
运行这3行代码来创建用户并授权：
CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY 'apppass';
GRANT ALL PRIVILEGES ON transaction_monitoring.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;



第一阶段 — 最小可用版本
Step 1: Transaction（交易）
→ 创建 Model / Repository / Service / Controller
→ 实现 POST /transactions（提交一笔交易）
→ 实现 GET /transactions（查看所有交易）

Step 2: MonitoringRule（监控规则）
→ 创建对应的四层代码
→ 先在数据库插入几条硬编码规则
→ 实现 GET /rules（查看规则）

Step 3: Alert + 规则引擎
→ 每次提交交易时，自动对所有启用的规则进行判断
→ 如果触发规则，自动创建 Alert 记录

Step 4: Alert 生命周期管理
→ PATCH /alerts/{id}/acknowledge
→ PATCH /alerts/{id}/investigate
→ PATCH /alerts/{id}/close
→ PATCH /alerts/{id}/dismiss



测试场景 1：金额阈值规则（AMOUNT_THRESHOLD）
目的：验证单笔交易超过 10000 时触发告警
POST http://localhost:8080/transactions

{
"accountId": "ACC-001",
"payeeId": "PAYEE-BANK",
"amount": 15000,
"transactionType": "DEBIT",
"description": "High value wire transfer"
}
预期结果：
交易被保存
同时自动生成一个 HIGH 级别的告警（对应 "High Value Transaction" 规则）

测试场景 2：新收款方规则（NEW_PAYEE）
目的：验证向从未出现过的收款方转账会触发告警
POST http://localhost:8080/transactions

{
"accountId": "ACC-002",
"payeeId": "PAYEE-UNKNOWN-NEW",
"amount": 500,
"transactionType": "DEBIT",
"description": "Payment to new vendor"
}
预期结果：
交易被保存
自动生成一个 LOW 级别的告警（对应 "New Payee" 规则）

测试场景 3：频率规则（VELOCITY）
目的：验证 10 分钟内多笔交易超过 5 笔时触发告警
快速发送这个请求 6 次以上（间隔几秒钟）：
POST http://localhost:8080/transactions

{
"accountId": "ACC-003",
"payeeId": "PAYEE-FAST",
"amount": 100,
"transactionType": "DEBIT",
"description": "Rapid transaction"
}
预期结果：
前 5 笔：正常保存，不触发告警
第 6 笔及以后：触发 MEDIUM 级别的告警（对应 "Rapid Transactions" 规则）

测试场景 4：每日限额规则（DAILY_LIMIT）
目的：验证同一账户当日累计金额超过 50000 时触发告警
连续发送多笔交易（每笔金额> 10000，累计> 50000）：
POST http://localhost:8080/transactions

{
"accountId": "ACC-004",
"payeeId": "PAYEE-HIGH",
"amount": 20000,
"transactionType": "DEBIT",
"description": "Large daily transfer 1"
}
重复发送 3 次（总计 60000），第 3 笔会触发测试
预期结果：
前两笔：可能触发金额阈值告警
第三笔：额外触发 HIGH 级别的每日限额告警