建议按业务纵向切片分工，而不是一个人只做前端、一个人只做后端。这样每个人都负责一个相对完整、可独立演示的模块，包括数据库、后端 API、业务逻辑、前端页面和测试。

推荐的三人分工

| 成员 | 主要模块 | 后端职责 | 前端职责 |
|---|---|---|---|
| 成员 A | 交易管理 | 交易写入、查询、过滤、分页；交易数据模型；触发规则评估入口 | 交易列表、搜索过滤、交易详情、测试数据生成 |
| 成员 B | 规则管理与规则引擎 | 规则 CRUD；规则策略实现；金额、速度、新收款人、每日限额规则 | 规则列表、新建/编辑规则、启停规则、动态参数表单 |
| 成员 C | 告警管理与仪表盘 | 告警生成与查询；生命周期；状态历史；统计接口 | 告警列表、告警详情、状态操作、Dashboard 统计 |

这种分法的好处是：每个人都有完整模块，每个模块又能独立展示成果。

成员 A：交易管理模块

成员 A 负责整个系统的入口：接收、保存和展示交易。

后端工作
• 设计 Transaction 数据模型。
• 实现创建交易接口。
• 实现交易列表、详情、分页、排序和过滤接口。
• 支持按以下条件查询：
  - 交易 ID
  - 账户 ID
  - 收款人
  - 日期范围
  - 金额范围
• 交易保存后调用统一的规则引擎入口。
• 返回该交易是否触发告警。
• 编写测试数据生成器或场景模拟接口。
• 添加必要的数据库索引，例如：
  - accountid
  - timestamp
  - (accountid, timestamp)
  - (accountid, payeeid)

前端工作
• 交易列表页面。
• 搜索、过滤、分页和排序。
• 交易详情页面或弹窗。
• 是否触发告警的视觉标识。
• 创建测试交易的表单。
• 可选：批量生成模拟交易页面。

最小可交付成果
用户在页面提交一笔交易。
后端成功保存交易。
交易显示在列表中。
若满足规则，系统生成告警。
用户可以从交易记录跳转到相关告警。

成员 B：规则管理与规则引擎模块

成员 B 负责系统的核心检测能力，工作量和技术复杂度相对较高。

后端工作
• 设计 MonitoringRule 数据模型。
• 实现规则的创建、查询、更新、删除和启停接口。
• 建立统一规则接口，例如：

``java
interface RuleEvaluator {
    boolean supports(RuleType type);
    EvaluationResult evaluate(
        MonitoringRule rule,
        Transaction transaction
    );
}
`

• 第一阶段实现：
  - Amount Threshold Rule
  - Velocity Rule
  - New Payee Rule
• 第二阶段实现：
  - Daily Limit Rule
• 将规则配置存储在数据库，而不是全部写死。
• 返回标准化的规则评估结果：
  - 是否触发
  - 触发原因
  - 实际值
  - 阈值
  - 相关交易
• 处理重复告警的基础问题。
• 编写各规则的单元测试。

前端工作
• 规则列表页面。
• 创建规则页面。
• 编辑规则页面。
• 激活和停用规则。
• 根据规则类型显示不同参数字段。

例如，选择 Velocity Rule 后显示：

| 参数 | 示例 |
|---|---|
| 最大交易数量 | 5 |
| 时间窗口 | 10 |
| 时间单位 | 分钟 |
| 分组依据 | 账户 |

• 表单校验。
• 删除规则前的确认。
• 显示规则类型、严重程度和状态。

最小可交付成果
用户能在页面创建金额阈值规则。
用户能启用或停用规则。
新交易进入后，后端读取所有启用的规则。
规则满足时返回标准评估结果。
评估结果交给告警模块创建告警。

成员 C：告警管理与仪表盘模块

成员 C 负责把检测结果转化为操作员可以调查和处理的告警。

后端工作
• 设计以下数据模型：
  - Alert
  - AlertStatusHistory
  - AlertTransaction，或等价的告警与交易关联关系
• 实现告警列表和详情接口。
• 支持按状态、严重程度、日期和规则过滤。
• 实现告警生命周期：

`text
OPEN -> ACKNOWLEDGED -> INVESTIGATING -> CLOSED
            |                 |
            +----> DISMISSED <-+
`

• 验证非法状态转换。
• 记录每次状态变化：
  - 原状态
  - 新状态
  - 操作时间
  - 处理备注
• 实现告警统计接口：
  - Open 数量
  - Acknowledged 数量
  - 今日告警数
  - 按严重程度统计
  - 按状态统计
  - 平均处理时间
• 编写生命周期和状态转换测试。

前端工作
• Dashboard 总览页。
• 告警列表页面。
• 严重程度和状态过滤。
• 告警详情页面。
• 关联交易列表。
• 告警状态时间线。
• 以下操作按钮：
  - Acknowledge
  - Start Investigation
  - Close
  - Dismiss
• 关闭或驳回时填写备注。
• 严重程度和状态的颜色标识。

最小可交付成果
规则引擎触发后创建 OPEN 告警。
用户可以在 Dashboard 看到新告警。
用户可以查看触发告警的交易和规则。
用户可以依次确认、调查、关闭告警。
所有状态变化都有历史记录。

三个模块如何协作

整体调用流程如下：

`mermaid
flowchart LR
    UI1[交易页面] --> TA[交易 API]
    TA --> TDB[(交易表)]
    TA --> RE[规则引擎]
    UI2[规则页面] --> RA[规则 API]
    RA --> RDB[(规则表)]
    RDB --> RE
    RE --> AS[告警服务]
    AS --> ADB[(告警与历史表)]
    UI3[告警和 Dashboard] --> AA[告警 API]
    AA --> ADB
`

核心边界应该明确：

• 成员 A负责把交易可靠地保存下来，并调用规则引擎。
• 成员 B负责判断一笔交易是否符合某条规则。
• 成员 C负责根据触发结果创建并管理告警。
• 告警模块不自己重新计算规则。
• 规则模块不直接控制前端告警状态。
• 交易模块不负责告警生命周期。

需要共同确定的接口契约

在开始各自开发前，三个人应先共同确定以下对象和 API，否则很容易在联调阶段发生冲突。

交易对象

`json
{
  "id": "TXN-001",
  "accountId": "ACC-001",
  "payeeId": "PAYEE-A",
  "amount": 15000.00,
  "currency": "USD",
  "type": "DEBIT",
  "timestamp": "2026-07-27T10:30:00Z",
  "description": "Test transaction"
}
`

金额建议后端使用 BigDecimal 或数据库 DECIMAL，不要使用浮点数。

规则对象

`json
{
  "id": 1,
  "name": "High value transaction",
  "type": "AMOUNT_THRESHOLD",
  "severity": "HIGH",
  "active": true,
  "parameters": {
    "threshold": 10000,
    "currency": "USD"
  }
}
`

规则评估结果

`json
{
  "triggered": true,
  "ruleId": 1,
  "severity": "HIGH",
  "reason": "Transaction amount 15000 exceeded threshold 10000",
  "transactionIds": ["TXN-001"],
  "observedValue": 15000,
  "thresholdValue": 10000
}
`

告警对象

`json
{
  "id": "ALT-001",
  "ruleId": 1,
  "ruleName": "High value transaction",
  "severity": "HIGH",
  "status": "OPEN",
  "reason": "Transaction amount 15000 exceeded threshold 10000",
  "transactionIds": ["TXN-001"],
  "createdAt": "2026-07-27T10:30:01Z"
}
`

建议的 API 归属
成员 A：交易 API

`text
POST   /api/transactions
GET    /api/transactions
GET    /api/transactions/{id}
POST   /api/transactions/simulate
`

交易列表可使用查询参数：

`text
GET /api/transactions?accountId=ACC-001&minAmount=1000&from=...&to=...&page=0&size=20
`

成员 B：规则 API

`text
POST   /api/rules
GET    /api/rules
GET    /api/rules/{id}
PUT    /api/rules/{id}
PATCH  /api/rules/{id}/activation
DELETE /api/rules/{id}
`

成员 C：告警 API

`text
GET    /api/alerts
GET    /api/alerts/{id}
POST   /api/alerts/{id}/acknowledge
POST   /api/alerts/{id}/investigate
POST   /api/alerts/{id}/close
POST   /api/alerts/{id}/dismiss
GET    /api/alerts/{id}/history
GET    /api/dashboard/summary
`

采用动作型生命周期接口，比允许前端任意 PATCH status 更安全，因为后端可以清楚验证每一种状态转换。

公共任务如何分配

除了各自模块，还需要明确项目级公共工作，避免出现“大家都以为别人会做”的情况。

| 公共任务 | 建议负责人 |
|---|---|
| 项目骨架、代码规范、Git 分支规范 | 成员 A |
| 数据库迁移和种子数据总体协调 | 成员 A |
| OpenAPI/Swagger 与 API DTO 一致性 | 成员 B |
| 全局异常处理和输入校验规范 | 成员 B |
| 前端路由、导航栏和整体 UI 风格 | 成员 C |
| Dashboard 演示场景和最终串联 | 成员 C |
| README 和运行说明 | 三人各写自己的模块，成员 A 汇总 |
| 端到端测试 | 三人共同完成 |
| 演示数据和 presentation | 三人共同完成 |

“负责人”不等于一个人包办，而是由这个人确保该事项最终完成。

开发顺序
第一阶段：共同搭建骨架

三个人一起完成：

• 确定技术栈。
• 确定数据库。
• 建立后端和前端项目。
• 定义核心实体、枚举和 API 契约。
• 配置数据库迁移。
• 配置 CORS、Swagger 和统一错误格式。
• 建立主分支保护和 Pull Request 流程。

第二阶段：完成最小闭环

先只做金额阈值规则：

`text
创建金额规则
-> 提交交易
-> 规则被触发
-> 生成 OPEN 告警
-> Dashboard 显示告警
-> Acknowledge
-> Investigate
-> Close
`

这个完整闭环应该优先于增加第二、第三种规则。只要该链路打通，项目就已经具备可演示的核心价值。

第三阶段：扩展各模块
• 成员 A 增加交易过滤、分页、模拟数据。
• 成员 B 增加 Velocity 和 New Payee。
• 成员 C 增加 Dashboard、历史时间线和统计。
• 三人分别补齐单元测试和集成测试。

第四阶段：高级功能

优先级建议如下：

Daily Limit Rule。
告警去重。
实时 Dashboard 更新。
性能优化和索引。
图表。
异步消息队列。

不建议一开始就使用 Kafka、RabbitMQ、微服务或机器学习。对于培训项目，一个结构清晰的模块化单体应用通常比未完成的复杂分布式架构更有说服力。

工作量平衡建议

三个模块的复杂度并不完全相同：

• 规则引擎后端逻辑最复杂。
• 告警模块前端交互和状态管理最多。
• 交易模块基础工作较直接，但查询、数据生成和性能任务可以补足工作量。

因此可以这样平衡：

| 成员 | 主体工作 | 补充工作 |
|---|---|---|
| A | 交易前后端 | 数据生成器、数据库索引、分页与性能测试 |
| B | 规则前后端 | Swagger、单元测试、统一校验 |
| C | 告警前后端 | Dashboard、前端公共布局、端到端演示 |

Git 协作方式

推荐使用功能分支，而不是长期保留三个互相隔离的大分支：

`text
main
develop
feature/transactions-api
feature/transactions-ui
feature/rules-api
feature/rules-ui
feature/alerts-api
feature/alerts-ui
`

基本规则：

每个任务使用独立分支。
小批量提交和小型 Pull Request。
至少由另一位成员 Review。
develop` 必须保持可运行。
每天至少进行一次集成，避免最后一天才合并。
数据库结构变化必须使用 migration，不要手动修改共享数据库。
修改公共 DTO 或 API 时，先与另外两人确认。

最终每个人可演示的内容
成员 A

“我负责交易模块。这里可以创建、搜索和过滤交易；交易保存后会自动进入监控流程，并显示是否关联了告警。”

成员 B

“我负责规则管理和规则引擎。用户可以配置并启停不同类型的规则；引擎使用统一接口评估交易，因此可以比较容易地扩展新规则。”

成员 C

“我负责告警生命周期和 Dashboard。告警从 OPEN 开始，可以被确认、调查、关闭或驳回，所有状态变化都会保留审计历史。”

这样三个人都有清晰的业务所有权，也都能展示数据模型、后端、前端、测试和设计决策，符合“每个人负责一个基本完整模块”的要求。