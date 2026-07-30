# TransactionService 测试失败修复说明（2026-07-30）

## 1. 背景

在 `feature/rule_engine` 分支执行 `mvn clean test` 时，`TransactionServiceTest` 出现多项失败。主要集中在：

- 交易类型兼容（`DEBIT`）
- 模拟交易生成参数校验
- 时间分布逻辑与时间窗口合法性校验

---

## 2. 失败现象

对应测试文件：`backend/src/test/java/com/example/monitoring/transaction/TransactionServiceTest.java`

1. `createTransaction_shouldPersistAndTriggerRuleEngine`
   - 报错：`Unsupported transactionType: DEBIT`
2. `generateMockTransactions_shouldRejectNonPositiveCount`
   - 期望：`count must be greater than 0`
   - 实际：`count must be between 1 and 10000`
3. `generateMockTransactions_shouldSpreadCreatedAtAcrossRangeWhenNoStepProvided`
   - 期望中间时间：`08:05`
   - 实际：`08:03:20`
4. `generateMockTransactions_shouldRejectInvalidTimeRange`
   - 期望：抛出 `IllegalArgumentException`
   - 实际：未抛出

---

## 3. 根因分析

### 3.1 交易类型不兼容历史入参

`TransactionType.from` 仅支持枚举值，未兼容历史常用值 `DEBIT/CREDIT`，导致测试与历史调用失败。

### 3.2 count 校验文案与测试约定不一致

服务里把上下限合并成同一条文案（`between 1 and 10000`），与测试预期的精确错误文案不一致。

### 3.3 默认时间步长公式导致分布偏移

未传 `stepSeconds` 时，使用 `totalSeconds / count`。该算法不会首尾对齐，导致中间点提前。

### 3.4 缺少“窗口容量不足”显式校验

传入 `count + stepSeconds + 时间窗口` 组合不合法时，旧逻辑通过截断时间继续生成，未抛错。

---

## 4. 修复内容

### 4.1 兼容历史交易类型

文件：`backend/src/main/java/com/example/monitoring/transaction/entity/TransactionType.java`

在 `from` 方法中增加别名映射：

- `DEBIT -> TRANSFER_OUT`
- `CREDIT -> DEPOSIT`

这样既不破坏当前枚举设计，也兼容历史请求数据。

### 4.2 调整 count 校验逻辑与文案

文件：`backend/src/main/java/com/example/monitoring/transaction/service/TransactionService.java`

将 count 校验拆分为两段：

- `count <= 0` 抛 `count must be greater than 0`
- `count > 10000` 抛 `count must not exceed 10000`

### 4.3 修复未指定 stepSeconds 时的时间分布

未指定 `stepSeconds` 时，改为首尾对齐的均匀分布：

```java
offsetSeconds = round(i * totalSeconds / (count - 1))
```

确保：

- 第一条命中 `startAt`
- 最后一条命中 `endAt`
- 中间点均匀分布

### 4.4 增加非法时间窗口校验

当显式提供 `stepSeconds` 时，增加校验：

```text
(count - 1) * stepSeconds <= duration(startAt, endAt)
```

不满足时抛出：

```text
time range must be large enough for count and stepSeconds
```

同时增加 `stepSeconds <= 0` 的参数校验。

---

## 5. 验证结果

执行命令：

```powershell
cd C:\RAC-awesomest-team\backend
mvn test
```

结果：

- `BUILD SUCCESS`
- `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`

---

## 6. 影响说明

### 正向影响

- 修复 `TransactionServiceTest` 全部失败项。
- 提升了交易类型解析的兼容性。
- 生成数据时间分布更符合业务预期。
- 非法参数组合可提前失败，避免生成被截断的异常数据。

### 兼容性注意

- 历史调用中的 `DEBIT/CREDIT` 将被兼容映射，不需要调用方立即改造。
- 对于非法时间窗口组合，系统行为从“隐式截断”变为“显式抛错”。

