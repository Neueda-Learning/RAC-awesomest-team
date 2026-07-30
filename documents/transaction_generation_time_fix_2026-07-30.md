# TransactionService 生成时间分布与时间窗口校验修复说明（2026-07-30）

## 1. 背景

在 `TransactionService.generateMockTransactions` 中，系统会按请求参数批量生成交易并触发规则引擎评估。近期单元测试中暴露出两类问题：

1. 未传 `stepSeconds` 时，时间分布不符合“首尾对齐的均匀分布”预期。
2. 传入 `count + stepSeconds + 时间窗口` 组合不合法时，没有抛出参数异常。

---

## 2. 失败现象（测试层面）

对应测试文件：`backend/src/test/java/com/example/monitoring/transaction/TransactionServiceTest.java`

### 2.1 时间分布异常

- 用例：`generateMockTransactions_shouldSpreadCreatedAtAcrossRangeWhenNoStepProvided`
- 场景：`count=3, startAt=08:00, endAt=08:10`
- 期望：`08:00, 08:05, 08:10`
- 实际：`08:00, 08:03:20, 08:06:40`（或最后被截断）

### 2.2 非法时间窗口未被拒绝

- 用例：`generateMockTransactions_shouldRejectInvalidTimeRange`
- 场景：`count=5, stepSeconds=30, startAt=10:00, endAt=10:01`
- 期望：抛出 `IllegalArgumentException`
- 实际：未抛异常（通过“超过 `endAt` 后强制截断”继续生成）

---

## 3. 错误原因

### 3.1 默认步长公式不正确

原实现使用：

```java
stepSeconds = totalSeconds / count
```

该公式会把区间切成 `count` 份，而不是 `count-1` 个间隔，导致中间点提前，最后一个点不一定落在 `endAt`。

### 3.2 对“窗口容量不足”缺少显式校验

当调用方显式传入 `stepSeconds` 时，理论上必须满足：

```text
(count - 1) * stepSeconds <= duration(startAt, endAt)
```

原实现未做该校验，而是把越界时间直接截断到 `endAt`，掩盖了非法输入。

---

## 4. 修复方案

修复位置：`backend/src/main/java/com/example/monitoring/transaction/service/TransactionService.java`

### 4.1 未传 `stepSeconds`：改为首尾对齐均匀分布

采用基于索引的线性分布：

```java
offsetSeconds = round(i * totalSeconds / (count - 1))
createdAt = startAt.plusSeconds(offsetSeconds)
```

效果：

- `i=0` 必然命中 `startAt`
- `i=count-1` 必然命中 `endAt`
- 中间数据在区间内均匀分布

### 4.2 传了 `stepSeconds`：增加严格校验

1. `stepSeconds <= 0` 时抛异常。
2. 若 `(count - 1) * stepSeconds > totalSeconds`，抛出：

```text
time range must be large enough for count and stepSeconds
```

这样可提前阻断错误输入，而不是生成“被截断”的脏数据。

### 4.3 保持边界行为可预期

- `count == 1` 时，直接使用 `startAt` 作为唯一时间点。
- 避免 silent clamp 掩盖参数问题。

---

## 5. 验证结果

修复后重新执行测试（`mvn clean test`）时，`TransactionServiceTest` 相关失败用例通过，项目测试集恢复通过。

> 备注：测试日志中可能出现 FX 相关降级日志（如 `restTemplate` 在单测注入为空时的提示），该日志不影响本次修复目标与断言结果。

---

## 6. 影响评估

### 正向影响

1. 生成数据的时间分布更符合业务直觉与测试预期。
2. 非法时间参数能被及时拒绝，错误更早暴露。
3. 避免将越界时间悄悄截断造成数据偏差。

### 兼容性说明

- 对于之前依赖“越界自动截断”的调用方，行为会从“继续生成”变为“抛异常”。
- 建议调用方在请求前先校验时间窗口与步长组合是否合法。

---

## 7. 建议的后续优化

1. 在 `GenerateTransactionsRequest` 上增加 Bean Validation（如 `@Min(1)`）以提前拦截非法参数。
2. 为 `stepSeconds <= 0` 场景补充单元测试。
3. 在 API 文档中明确 `count/startAt/endAt/stepSeconds` 的组合约束。
