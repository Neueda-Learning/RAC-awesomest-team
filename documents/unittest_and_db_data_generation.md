# unittest 与数据库测试数据生成功能改动说明

## 1. 本次改动内容

### 1.1 单元测试补充
已补齐以下测试类（均为 Mockito + JUnit5 的纯单元测试，不依赖真实数据库）：

- `backend/src/test/java/com/example/monitoring/transaction/TransactionServiceTest.java`
  - 校验创建交易时默认币种回退为 `USD`
  - 校验保存交易后会触发规则引擎
  - 校验筛选逻辑分支（金额+时间组合）
  - 校验筛选参数非法时抛出异常
  - 校验自动生成交易数量与规则评估触发次数

- `backend/src/test/java/com/example/monitoring/rule/RuleServiceTest.java`
  - 校验创建规则时自动补齐 `createdAt/updatedAt`
  - 校验已有时间戳不会被覆盖
  - 校验更新规则字段映射正确
  - 校验规则不存在时抛出异常

- `backend/src/test/java/com/example/monitoring/alert/AlertServiceTest.java`
  - 校验 `OPEN -> ACKNOWLEDGED` 状态流转
  - 校验状态流转时会写入历史记录
  - 校验非法状态流转会抛出异常
  - 校验 `INVESTIGATING -> CLOSED` 合法流转

### 1.2 自动生成数据库数据方法
新增交易自动生成功能（用于联调与演示数据准备）：

- `backend/src/main/java/com/example/monitoring/transaction/service/TransactionService.java`
  - 新增 `generateMockTransactions(int count)`
  - 新增可配置版本，支持控制金额范围与生成时间
  - 随机生成账户、收款方、金额、交易类型、描述
  - 复用 `createTransaction(...)` 保存交易，因此会自动触发规则评估与告警生成

- `backend/src/main/java/com/example/monitoring/transaction/controller/TransactionController.java`
  - 新增接口：`POST /transactions/generate?count=100`
  - 支持通过查询参数控制金额和 `createdAt` 生成范围
  - 返回生成条数与生成时间

- `backend/src/main/java/com/example/monitoring/transaction/dto/GenerateTransactionsRequest.java`
  - 新增生成参数 DTO

- `backend/src/main/java/com/example/monitoring/transaction/dto/GenerateTransactionsResponse.java`
  - 新增返回 DTO

## 2. 如何测试

### 2.1 运行单元测试
在 `backend` 目录执行：

```powershell
mvn test
```

### 2.2 生成数据库测试数据
启动后端服务后，调用：

```powershell
curl -X POST "http://localhost:8080/transactions/generate?count=100"
```

如果需要控制金额范围和时间范围，可以这样调用：

```powershell
curl -X POST "http://localhost:8080/transactions/generate?count=20&minAmount=100&maxAmount=500&startAt=2026-07-28T10:00:00&stepSeconds=120"
```

或指定一个时间区间，让系统自动均匀分布 `createdAt`：

```powershell
curl -X POST "http://localhost:8080/transactions/generate?count=20&minAmount=100&maxAmount=500&startAt=2026-07-28T10:00:00&endAt=2026-07-28T11:00:00"
```

可选参数说明：

- `count`：生成数量，默认 `100`
- `minAmount`：最小金额
- `maxAmount`：最大金额
- `startAt`：生成起始时间（ISO-8601 格式）
- `endAt`：生成结束时间（ISO-8601 格式）
- `stepSeconds`：相邻两笔交易之间的秒数间隔

说明：

- 只传 `startAt + stepSeconds`：按固定步长递增生成时间，适合避免时间完全相同
- 传 `startAt + endAt`：在区间内均匀分布时间
- 同时传 `startAt + endAt + stepSeconds`：会按步长递增，并校验区间是否足够容纳所有记录

示例返回（字段含义）：

- `generatedCount`: 实际生成的交易条数
- `generatedAt`: 生成时间

### 2.3 验证生成结果
可通过以下接口快速验证：

```powershell
curl "http://localhost:8080/transactions"
curl "http://localhost:8080/alerts"
```

如果规则命中，`/alerts` 会出现新增告警。

## 3. 注意事项

- 生成接口用于测试/演示场景，建议仅在开发或联调环境使用。
- 当 `count <= 0` 时，接口会触发参数异常（服务端会返回错误信息）。
- 随机交易会触发规则引擎，生成较大数据量时请按环境容量分批执行。

