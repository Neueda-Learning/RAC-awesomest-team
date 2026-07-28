# Bugfix: 规则引擎多币种金额换算

**日期**：2026-07-28  
**分支**：feature/rule_engine  
**涉及模块**：规则引擎、交易服务、公共服务

---

## 问题描述

规则引擎在执行 `AMOUNT_THRESHOLD`（单笔限额）和 `DAILY_LIMIT`（日累计限额）规则时，直接比较交易的原始金额数值与阈值，未进行币种换算。

**复现示例**：
- 规则：单笔超过 10000 USD 触发告警
- 交易：9999 EUR（实际约合 11000+ USD）
- 结果：未触发告警（9999 < 10000，数值比较通过）

## 根本原因

`RuleEngineService.checkAmountThreshold` 和 `checkDailyLimit` 直接使用 `tx.getAmount()` 与阈值做数字比较，忽略了 `currency` 字段。

`TransactionService` 中虽已有汇率获取和币种转换的私有方法（`getFxRates` / `convertToUsd`），但这些方法只被 `filterTransactions` 使用，规则引擎无法访问。

---

## 修复方案

### 1. 新增 `FxService`

路径：`src/main/java/com/example/monitoring/common/service/FxService.java`

将汇率获取、缓存（5 分钟 TTL）、币种转换逻辑从 `TransactionService` 抽取为独立的 Spring Bean，供多个模块复用。

核心方法：
- `convertToUsd(BigDecimal amount, String currency)` — 通过 openexchangerates API 将任意币种金额换算为 USD，汇率获取失败时返回 `null`
- `getRates()` — 带缓存的汇率数据获取

### 2. 重构 `TransactionService`

- 注入 `FxService`，删除原有私有方法 `getFxRates` 和 `convertToUsd`
- `filterTransactions` 改用 `fxService.convertToUsd()`

### 3. 更新 `RuleEngineService`

- 注入 `FxService`
- `checkAmountThreshold`：先将交易金额转为 USD，再与阈值比较；汇率获取失败时返回 `false`（不误报）
- `checkDailyLimit`：查询当日所有交易，每笔转为 USD 后求和，再与阈值比较

### 4. 新增 `TransactionRepository` 查询方法

新增 `findByAccountIdAndCreatedAtAfter`，供 `checkDailyLimit` 获取当日交易列表进行逐笔币种转换。

---

## 修改文件清单

| 文件 | 变更类型 |
|------|----------|
| `common/service/FxService.java` | 新增 |
| `transaction/service/TransactionService.java` | 重构（移除私有 FX 方法，改用 FxService） |
| `rule/service/RuleEngineService.java` | 修复（注入 FxService，更新金额比较逻辑） |
| `transaction/repository/TransactionRepository.java` | 新增查询方法 |

---

## 降级策略

若 openexchangerates API 不可用且缓存已过期：
- `checkAmountThreshold`：返回 `false`，不触发告警（避免误报）
- `checkDailyLimit`：该笔交易的金额被跳过，不计入日累计总额
