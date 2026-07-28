# Rule Engine Frontend Bug Fix Report

**Date:** 2026-07-27  
**File Fixed:** `backend/src/main/resources/static/rule_engine.html`  



---

## 问题描述

`rule_engine.html` 页面打开后，数据库中已有的四条监控规则（High Value Transaction、Rapid Transactions、New Payee、Daily Limit Exceeded）无法在列表中显示。

---

## 根本原因分析

### Bug 1：`isActive` 字段名与 Jackson 序列化不一致（核心 Bug）

**位置：** `MonitoringRule.java`

```java
// Java 字段声明
private boolean isActive;

// getter 方法
public boolean isActive() { return isActive; }

// setter 方法
public void setActive(boolean active) { isActive = active; }
```

**Jackson 的序列化规则：**

Jackson 对 `boolean` 类型的 getter 有特殊处理：
- 当 getter 方法名以 `is` 开头（如 `isActive()`），Jackson **自动去掉 `is` 前缀**，将属性名序列化为 `active`。
- 因此 `GET /rules` 返回的 JSON 字段名是 `"active"`，而不是 `"isActive"`。

**实际 API 返回示例：**
```json
{
  "id": 1,
  "ruleName": "High Value Transaction",
  "ruleType": "AMOUNT_THRESHOLD",
  "severity": "LOW",
  "active": true,
  "thresholdValue": 10000.00
}
```

**前端原始错误代码（POST/PUT payload）：**
```javascript
// 错误：发送 "isActive" 但 Java setter 是 setActive()，对应 "active"
const payload = { ruleName, ruleType, severity, isActive, ...extraParams };
```

由于 Java 的 setter 是 `setActive()`（对应 JSON key `"active"`），发送 `"isActive"` 时 Jackson 找不到对应的 setter，导致：
- 创建规则时 `isActive` 字段永远为默认值 `false`
- 更新规则时 active 状态修改无效

---

### Bug 2：错误状态不可见（用户体验问题）

**原始错误处理：**
```javascript
} catch (e) {
    showToast('Failed to load rules: ' + e.message, 'error');
    document.getElementById('rules-empty').style.display = 'block';
}
```

**问题：**
- Toast 消息在 3.5 秒后自动消失
- 消失后页面仅显示"No rules found"，用户无法判断是真的没有规则，还是后端未启动
- 当 Spring Boot 应用未运行时，用户不知道原因

---

### Bug 3：Toggle 开关的字段名不确定性

**原始 Toggle 代码：**
```javascript
const fieldName = rule.isActive !== undefined ? 'isActive' : 'active';
const payload = { ...rule };
payload[fieldName] = isActive;
```

**问题：**
- 由于 GET 返回的是 `"active"` 字段，`rule.isActive` 永远是 `undefined`
- `fieldName` 虽然会正确选择 `'active'`，但逻辑依赖 `undefined` 判断，不够清晰且易出错

---

## 修复方案

### Fix 1：统一 POST/PUT payload 使用 `active` 字段名

**修复前（submitRule）：**
```javascript
const payload = { ruleName, ruleType, severity, isActive, ...extraParams };
```

**修复后（submitRule）：**
```javascript
const payload = { ruleName, ruleType, severity, active: isActive, ...extraParams };
```

**修复前（saveEdit）：**
```javascript
const payload = { ruleName, ruleType, severity, isActive, ...extraParams };
```

**修复后（saveEdit）：**
```javascript
const payload = { ruleName, ruleType, severity, active: isActive, ...extraParams };
```

---

### Fix 2：改进错误状态显示

**修复后的错误处理：**
```javascript
} catch (e) {
    const isNetworkError = e.message === 'Failed to fetch' || e.message.includes('NetworkError');
    const msg = isNetworkError
        ? '⚠️ Cannot connect to backend. Please make sure the Spring Boot application is running on port 8080.'
        : 'Failed to load rules: ' + e.message;
    document.getElementById('rules-empty').innerHTML =
        `<div class="empty-icon">🔌</div>
         <p style="color:#e94560;font-weight:700;">${isNetworkError ? 'Backend Not Running' : 'Load Failed'}</p>
         <p>${msg}</p>
         ${isNetworkError ? '<p>Run: <code>mvn spring-boot:run</code></p>' : ''}
         <button onclick="loadRules()">🔄 Retry</button>`;
    document.getElementById('rules-empty').style.display = 'block';
}
```

**改进点：**
- 区分"网络错误（后端未启动）"和"其他错误"
- 错误信息持久显示，不会自动消失
- 网络错误时显示启动命令 `mvn spring-boot:run`
- 提供"Retry"重试按钮，无需刷新页面

---

### Fix 3：简化并统一 Toggle 开关逻辑

**修复前：**
```javascript
const fieldName = rule.isActive !== undefined ? 'isActive' : 'active';
const payload = { ...rule };
payload[fieldName] = isActive;
```

**修复后：**
```javascript
const payload = { ...rule, active: isActive };
delete payload.isActive; // 清除可能存在的 isActive key
```

**改进点：**
- 始终使用 `active` 字段，与 Jackson 序列化结果完全一致
- 删除可能混入的 `isActive` key，避免发送多余字段

---

### Fix 4：加强 `isActive` 字段的读取逻辑

**修复前（renderRuleRow）：**
```javascript
const isActive = r.active !== undefined ? r.active : r.isActive;
```

**修复后：**
```javascript
const isActive = r.active !== undefined ? r.active : (r.isActive !== undefined ? r.isActive : false);
```

**修复前（loadRules 统计）：**
```javascript
const active = rules.filter(r => r.active || r.isActive).length;
```

**修复后：**
```javascript
const active = rules.filter(r => r.active === true || r.isActive === true).length;
```

**改进点：**
- 使用严格的 `=== true` 判断，避免 `false || undefined` 被当做 `undefined` 处理
- 增加默认值 `false`，防止字段缺失时出现 `undefined` 问题

---

## 验证结果

修复后通过 PowerShell 验证 API 实际返回字段名：

```powershell
$r = Invoke-RestMethod "http://localhost:8080/rules"
$r | ForEach-Object { Write-Output "$($_.id) $($_.ruleName) active=$($_.active)" }
```

**输出：**
```
1 High Value Transaction active=True
2 Rapid Transactions active=True
3 New Payee active=True
4 Daily Limit Exceeded active=True
```

确认字段名为 `active`（不是 `isActive`），四条规则均正常返回并在页面显示。

---

## 经验总结

| 知识点 | 说明 |
|---|---|
| Jackson boolean 序列化 | `boolean isXxx()` getter → JSON 字段名 `xxx`（自动去掉 `is` 前缀） |
| Spring Data JDBC 映射 | 字段 `isActive` → 数据库列 `is_active`（驼峰转下划线） |
| 前端字段名要匹配 | POST/PUT 的 JSON key 必须与后端 setter 对应的属性名一致 |
| 错误处理最佳实践 | 网络错误应持久显示，区分"后端未启动"和"数据错误" |

---

## 正确的访问方式

页面必须通过 Spring Boot 服务器访问，**不能**直接双击文件打开：

```
✅ 正确：http://localhost:8080/rule_engine.html
❌ 错误：file:///C:/RAC-awesomest-team/backend/src/main/resources/static/rule_engine.html

```

直接用 `file://` 打开会因为浏览器 CORS 安全策略导致 fetch 请求失败。

---

## 增量补充（仅新增，不改动原记录）

### 1) 重构后路径现状

- 当前页面文件位于：
  - `backend/src/main/resources/static/rule_engine.html`
  - `backend/src/main/resources/static/transactions.html`
  - `backend/src/main/resources/static/alerts.html`
- 推荐访问入口：
  - `http://localhost:8080/rule_engine.html`
  - `http://localhost:8080/transactions.html`
  - `http://localhost:8080/alerts.html`

### 2) API 路由约定（当前实现）

- 当前控制器实际路由不带 `/api` 前缀：
  - Rules: `/rules`
  - Transactions: `/transactions`
  - Alerts: `/alerts`
- 若后续统一改为 `/api/...`，需要同步更新三个静态页面中的 `fetch` 地址。

### 3) 三页互跳导航补充

- 侧边栏 Quick Links 已支持页面互跳：
  - `rule_engine.html` 可跳转 `transactions.html`、`alerts.html`
  - `transactions.html` 可跳转 `rule_engine.html`、`alerts.html`
  - `alerts.html` 可跳转 `rule_engine.html`、`transactions.html`

### 4) 新增规则 500 问题关联说明

- 现象：规则新增请求 `POST /rules` 可能返回 500。
- 已处理：在 `backend/src/main/java/com/example/monitoring/rule/service/RuleService.java` 的 `createRule` 中补充 `createdAt/updatedAt` 默认时间戳初始化。
- 说明：该问题与本报告的前端字段名修复不同，属于后端插入数据完整性问题，二者都需要保留记录。

