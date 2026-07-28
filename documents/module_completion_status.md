# 当前模块功能完成情况（截至 2026-07-28）

## 0. 系统边界说明

- 本系统默认只有一个用户角色：**管理员（单用户）**。
- 当前实现中**不包含**登录、鉴权、用户管理、角色权限等身份相关功能。

## 1. Transaction 模块（交易）

对应目录：
- `backend/src/main/java/com/example/monitoring/transaction/controller`
- `backend/src/main/java/com/example/monitoring/transaction/service`
- `backend/src/main/java/com/example/monitoring/transaction/repository`
- `backend/src/main/java/com/example/monitoring/transaction/entity`
- `backend/src/main/java/com/example/monitoring/transaction/dto`

已完成功能：
1. 交易创建
   - `POST /transactions`
   - 支持入参校验（`accountId/payeeId/amount/transactionType`）
   - 创建成功后返回 `201`

2. 交易查询
   - `GET /transactions`：查询全部交易
   - `GET /transactions/{id}`：按 ID 查询
   - `GET /transactions?accountId=...`：按账户查询

3. 交易搜索与筛选
   - `GET /transactions/search?keyword=...`：按描述模糊搜索
   - `GET /transactions/filter?...`：按金额区间和/或时间区间筛选
   - 已实现参数合法性校验（如 `minAmount <= maxAmount`、`from <= to`）

4. 自动生成测试数据
   - `POST /transactions/generate`
   - 支持参数：`count`、`minAmount`、`maxAmount`、`startAt`、`endAt`、`stepSeconds`
   - 支持控制 `createdAt` 分布（固定步长或时间区间均匀分布）

5. 触发规则评估
   - 每次创建交易后，都会同步调用规则引擎进行评估

## 2. Rule 模块（监控规则）

对应目录：
- `backend/src/main/java/com/example/monitoring/rule/controller`
- `backend/src/main/java/com/example/monitoring/rule/service`
- `backend/src/main/java/com/example/monitoring/rule/repository`
- `backend/src/main/java/com/example/monitoring/rule/entity`

已完成功能：
1. 规则 CRUD
   - `GET /rules`
   - `GET /rules/{id}`
   - `POST /rules`
   - `PUT /rules/{id}`
   - `DELETE /rules/{id}`

2. 规则创建时间补全
   - 创建规则时自动补齐 `createdAt` / `updatedAt`

3. 规则引擎（RuleEngineService）
   - 对启用规则逐条评估，命中后自动生成告警
   - 已实现 4 类规则：
     - `AMOUNT_THRESHOLD`
     - `VELOCITY`
     - `NEW_PAYEE`
     - `DAILY_LIMIT`

4. 初始规则数据
   - `schema.sql` 已内置四类规则初始化 SQL（幂等插入）

## 3. Alert 模块（告警）

对应目录：
- `backend/src/main/java/com/example/monitoring/alert/controller`
- `backend/src/main/java/com/example/monitoring/alert/service`
- `backend/src/main/java/com/example/monitoring/alert/repository`
- `backend/src/main/java/com/example/monitoring/alert/entity`
- `backend/src/main/java/com/example/monitoring/alert/dto`

已完成功能：
1. 告警查询
   - `GET /alerts`：查询全部告警
   - `GET /alerts?status=...`：按状态筛选
   - `GET /alerts/{id}`：按 ID 查询
   - `GET /alerts/{id}/history`：查询告警状态历史

2. 告警生命周期流转
   - `PATCH /alerts/{id}/acknowledge`（OPEN -> ACKNOWLEDGED）
   - `PATCH /alerts/{id}/investigate`（ACKNOWLEDGED -> INVESTIGATING）
   - `PATCH /alerts/{id}/close`（INVESTIGATING -> CLOSED）
   - `PATCH /alerts/{id}/dismiss`（ACKNOWLEDGED/INVESTIGATING -> DISMISSED）

3. 状态流转校验
   - 非法流转会抛出业务异常

4. 状态历史审计
   - 每次流转都会写入 `alert_status_history`

## 4. Common 模块（通用异常处理）

对应目录：
- `backend/src/main/java/com/example/monitoring/common/exception`

已完成功能：
1. 全局异常处理器 `GlobalExceptionHandler`
2. 参数校验异常（`MethodArgumentNotValidException`）统一返回 `400`
3. 业务异常映射：
   - 参数范围类错误映射 `400`
   - 资源不存在类错误映射 `404`
   - 状态流转错误映射 `409`

## 5. 数据库与初始化

对应目录：
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/application.properties`

已完成功能：
1. 核心表结构已完成
   - `transaction`
   - `monitoring_rule`
   - `alert`
   - `alert_status_history`

2. 核心索引与外键已配置
3. 规则初始化 SQL 与告警严重级别修正 SQL 已配置
4. Spring 启动时执行 SQL 初始化（`spring.sql.init.mode=always`）

## 6. 前端调试页面（后端静态托管）

对应目录：
- `backend/src/main/resources/static/transactions.html`
- `backend/src/main/resources/static/rule_engine.html`
- `backend/src/main/resources/static/alerts.html`

已完成功能：
1. 三个调试页面已接入后端静态资源托管
2. 可直接通过 `http://localhost:8080/*.html` 访问
3. 用于联调交易、规则、告警相关接口

## 7. 测试模块

对应目录：
- `backend/src/test/java/com/example/monitoring/transaction/TransactionServiceTest.java`
- `backend/src/test/java/com/example/monitoring/rule/RuleServiceTest.java`
- `backend/src/test/java/com/example/monitoring/alert/AlertServiceTest.java`

已完成功能：
1. TransactionService 单元测试
   - 创建交易、筛选逻辑、参数校验、数据生成策略
2. RuleService 单元测试
   - 规则创建时间戳补全、更新逻辑、异常路径
3. AlertService 单元测试
   - 生命周期流转、非法流转、历史记录写入

## 8. 运维与工程化（当前已落地）

1. 已修复 `backend/target` 误跟踪问题并完善 `.gitignore`
2. 已增加优雅停机配置，降低端口残留占用概率：
   - `server.shutdown=graceful`
   - `spring.lifecycle.timeout-per-shutdown-phase=20s`
   - `spring.main.register-shutdown-hook=true`
3. Java 版本与 Maven 编译版本已统一到 `Java 17`

## 9. 当前已明确不在范围内

1. 用户身份与权限（登录、鉴权、多用户、RBAC）
2. 告警分派到具体人员（因单管理员模式暂不需要）
3. 通知系统（邮件/短信/Webhook）
4. 实时推送（WebSocket/SSE）
5. 复杂规则编排（AND/OR 组合规则）

---

如需后续迭代，可基于本文件继续补充“下一阶段计划”与“负责人分工”。

