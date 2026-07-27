# Transaction Monitoring & Alerts Dashboard Training Project
交易监控与告警看板培训项目

[TOC]
[目录]

## Overview
## 概述

Your team is challenged with designing a transaction monitoring and alerting system that detects suspicious or unusual transaction patterns in real-time.
你们的团队需要设计一个交易监控与告警系统，能够实时检测可疑或异常的交易模式。

The system will continuously evaluate transactions against configurable rules and generate alerts when thresholds or patterns are detected, maintaining a full audit trail of all alerts and their resolutions.
系统会持续根据可配置规则评估交易；当检测到阈值超限或异常模式时生成告警，并保留所有告警及其处理结果的完整审计轨迹。

Your task is to build the application.
你们的任务是把这个应用构建出来。

## Technical Goals
## 技术目标

You should aim to create a Transaction Monitoring REST API and dashboard.
你们应当构建一个交易监控 REST API 和一个仪表板。

This API should allow storing transactions, defining monitoring rules, generating alerts, and managing the alert lifecycle.
该 API 应支持交易存储、监控规则定义、告警生成以及告警生命周期管理。

If/When you have made progress on the core requirements then requirements for further enhancements will be provided. This will include open-ended enhancements whereby you can make use of your particular skills and experience.
当你们在核心需求上取得进展后，会提供进一步增强的需求，其中会包含一些开放式扩展，便于你们发挥各自的技能和经验。

We will continue working on this project into the week where you start looking at Web front ends.
在你们开始学习 Web 前端的那一周，我们也会继续推进这个项目。

For the Front end, you can use the technologies you have learned about in your training. If you wish to use a specific framework or some other technology, please check with your instructor.
前端部分可以使用你们在培训中学到的技术；如果想使用特定框架或其他技术，请先和讲师确认。

The Front end should facilitate your users to (in order of priority):
前端应帮助用户完成以下功能（按优先级排序）：

* View all transactions (with filtering and search)
  查看所有交易（支持筛选和搜索）
* View active alerts
  查看当前活跃告警
* View alert details and triggering transactions
  查看告警详情及触发该告警的交易
* Acknowledge alerts
  确认（Acknowledge）告警
* Close or dismiss alerts
  关闭或驳回告警
* View alert history
  查看告警历史
* View and edit monitoring rules
  查看并编辑监控规则

In terms of detailed requirements, your instructor will act as customer, and will tell you what they want. You can arrange meetings with them as required.
在详细需求方面，讲师会扮演“客户”角色并告知你们具体诉求；你们可以按需要与其安排沟通会议。

## Alert Lifecycle
## 告警生命周期

Your system should implement the following alert workflow:
你的系统应实现如下告警流转流程：

```
OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED
         ↓                 ↓
    DISMISSED         DISMISSED
```

**Status Definitions:**
**状态定义：**

* **OPEN** - Alert has been generated but not yet reviewed
  **OPEN** - 告警已生成，但尚未被审阅
* **ACKNOWLEDGED** - Alert has been seen by an operator but not yet investigated
  **ACKNOWLEDGED** - 告警已被操作员看到，但尚未开始调查
* **INVESTIGATING** - Alert is actively being investigated
  **INVESTIGATING** - 告警正在被积极调查中
* **CLOSED** - Investigation complete, issue resolved or confirmed legitimate
  **CLOSED** - 调查完成，问题已解决或已确认交易合法
* **DISMISSED** - Alert determined to be false positive or not requiring action
  **DISMISSED** - 告警被判定为误报或无需进一步处理

## Core Requirements
## 核心需求

### API Endpoints
### API 接口

Design the REST API surface yourselves - decide what operations, routes, and HTTP methods make sense for recording transactions, managing alerts through their lifecycle, and managing monitoring rules. Use the REST API technology taught in your class (e.g., Spring Boot, Flask, Express.js, etc.).
请你们自行设计 REST API 接口面：决定哪些操作、路由和 HTTP 方法最适合用于记录交易、管理告警生命周期以及管理监控规则。请使用课程中教授的 REST API 技术（例如 Spring Boot、Flask、Express.js 等）。

## Rule Types
## 规则类型

Your system should support these basic rule types:
你的系统应支持以下基础规则类型：

### 1. Amount Threshold Rule
### 1. 金额阈值规则
Trigger alert when a single transaction exceeds a threshold amount.
当单笔交易金额超过阈值时触发告警。

**Example:** Alert if any transaction > $10,000
**示例：** 任意单笔交易 > 10,000 美元时触发告警。

### 2. Velocity Rule
### 2. 交易频率（Velocity）规则
Trigger alert when N transactions occur within T time period.
当在 T 时间窗口内发生 N 笔交易时触发告警。

**Example:** Alert if more than 5 transactions occur within 10 minutes from the same account
**示例：** 同一账户 10 分钟内交易超过 5 笔时触发告警。

### 3. New Payee Rule
### 3. 新收款方规则
Trigger alert when a transaction is made to a previously unseen payee/counterparty.
当交易转给此前从未出现过的收款方/交易对手时触发告警。

**Example:** Alert on first transaction to any new payee from an account
**示例：** 某账户首次向任何新收款方转账时触发告警。

### 4. Daily Limit Rule (Advanced)
### 4. 每日限额规则（进阶）
Trigger alert when cumulative transaction amount exceeds daily limit.
当单日累计交易金额超过每日限额时触发告警。

**Example:** Alert if total transactions from an account exceed $50,000 in one day
**示例：** 某账户单日总交易金额超过 50,000 美元时触发告警。

## Notes
## 说明

1. There will be no authentication and a single operator is assumed, i.e. there is no requirement to manage multiple users or operators.
   本项目不要求认证，默认只有一个操作员；也就是说无需管理多个用户或操作员。

2. You should use the database technology you have been using in the training for any persistent storage.
   持久化存储应使用你们培训中一直在使用的数据库技术。

3. Make good use of git. Use branching and pull requests if you can.
   请充分使用 Git；如果可以，采用分支和 Pull Request 工作流。

4. Any documentation about how to use your REST API would be useful. Maybe Swagger/OpenAPI if you have covered it in your training.
   提供 REST API 使用文档会非常有帮助；如果培训里学过，可考虑 Swagger/OpenAPI。

5. For this training project, transactions can be generated via API calls or through a simple data generator/simulator.
   在该培训项目中，交易数据可通过 API 调用生成，或通过简单的数据生成器/模拟器生成。

6. Rules can initially be hardcoded, but implementing configurable rules is a great enhancement.
   规则起步阶段可以硬编码，但实现“可配置规则”会是很好的增强。

## Technical Getting Started Checklist
## 技术启动清单

1. Create your project structure.
   创建你的项目结构。

2. Create a Git repository. Your instructors will guide you as to which Git platform to use.
   创建 Git 仓库。讲师会指导你们使用哪个 Git 平台。

3. Add, commit, push your skeleton project to your Git repository.
   将项目骨架代码 add、commit 并 push 到仓库。

4. Ensure your team has access to the Git repository.
   确保团队成员都有仓库访问权限。

5. Decide on the absolute MINIMUM fields for a first working system e.g. the first version may just have transactions with id, amount, timestamp, and a single hardcoded rule for amount threshold.
   明确第一个可运行版本所需的“最小字段集合”；例如第一版只包含交易 id、金额、时间戳，以及一个硬编码的金额阈值规则。

If you get stuck getting any of the above completed then contact your instructor for help.
如果上述任何事项推进受阻，请联系讲师寻求帮助。

## Project Management Getting Started Checklist
## 项目管理启动清单

1. As a team decide how you will approach the work. E.g. 2 people on the backend, 1 person on UI Design Vs. Everyone on the backend until a basic system is working.
   团队先决定协作方式，例如：2 人做后端、1 人做 UI 设计；或先全部投入后端，直到基础系统跑通。

2. Make a task list. Ideally use a tool such as Trello to keep track of tasks.
   制作任务清单，最好使用 Trello 等工具跟踪任务。

3. Some of your team may work on the DESIGN of a more fully-featured application, while some of your team work on BUILDING some small pieces as demonstration.
   团队中可有人负责更完整应用的设计，也可有人先实现小功能作为演示。

4. Choose the tasks required for a MINIMAL implementation first.
   先做实现最小可用版本所必需的任务。

5. Your instructor will drop in regularly to see how you're progressing. Make a note of any questions so that you're ready to ask them then.
   讲师会定期跟进你们进度；请提前记录问题，方便当面请教。

6. Your team should get together and decide on an initial set of data that you will store. A good team decision on this is a good path to success, however remember to STAY AGILE.
   团队应共同确定初始要存储的数据集合。这个决策做得好会大幅提高成功率，但也要保持敏捷（AGILE）。
The single biggest problem teams face is starting out with a data model that is too complex.
团队最常见且最严重的问题之一，是一开始就采用了过于复杂的数据模型。

## Suggestions for Success
## 成功建议

1. START SMALL. Get a system working that stores transactions and implements ONE simple rule (e.g., amount threshold). You can then add more sophisticated rules.
   从小处开始。先做一个能存交易并支持一个简单规则（如金额阈值）的系统，然后再逐步增加复杂规则。

2. Try pair programming, it can be very effective.
   试试结对编程，这通常非常有效。

3. Take conscious steps to keep a good energy in the team. E.g. give your team a name, systematically plan check-ins with each other.
   有意识地维护团队氛围，例如给团队起个名字，定期安排彼此的同步沟通。

4. Emphasise quality over quantity.
   重质量，轻数量。

5. Think about performance early - what if you need to check 1000 transactions per second against multiple rules?
   尽早考虑性能问题：如果每秒要对 1000 笔交易进行多规则校验怎么办？

6. Consider separation of concerns - transaction recording should be fast and independent from rule evaluation.
   考虑关注点分离：交易记录应尽量快速，并与规则评估解耦。

### Event-Driven Architecture
### 事件驱动架构
* Should rule evaluation happen synchronously when a transaction is recorded?
  规则评估应在交易记录时同步执行吗？
* Consider using an event/message queue for asynchronous processing. This will require some research by you.
  考虑使用事件/消息队列进行异步处理，这需要你们做一些调研。
* Decoupling transaction recording from alert generation
  将交易记录与告警生成解耦。
* What are the tradeoffs between real-time and near-real-time processing?
  实时与准实时处理之间有哪些取舍？

### Rule Engine Patterns
### 规则引擎模式
* How do you design a flexible rule engine?
  如何设计一个灵活的规则引擎？
* Should rules be hardcoded or configurable?
  规则应硬编码还是可配置？
* Consider the Strategy pattern or Chain of Responsibility pattern
  可考虑策略模式（Strategy）或责任链模式（Chain of Responsibility）。
* How do you make it easy to add new rule types without changing core code?
  如何在不改核心代码的情况下，轻松增加新规则类型？

### Real-time vs Batch Processing
### 实时处理 vs 批处理
* Should rules evaluate every transaction immediately?
  是否每笔交易都要立即评估规则？
* Or should they run periodically (e.g., every minute) in batch?
  还是应周期性（如每分钟）批量执行？
* What are the performance implications?
  这对性能有什么影响？
* Consider hybrid approaches for different rule types
  对不同规则类型可以考虑混合处理方案。

### Alert Fatigue / Priority Levels
### 告警疲劳 / 优先级
* Too many alerts = operators ignore them
  告警太多 = 操作员会忽略它们。
* How do you prioritize alerts? (HIGH/MEDIUM/LOW severity)
  如何给告警排优先级？（HIGH/MEDIUM/LOW 严重级别）
* Should repeated similar alerts be grouped?
  重复且相似的告警是否应归并？
* Consider alert throttling or deduplication
  可考虑告警限流（throttling）或去重（deduplication）。

### Database Query Optimization
### 数据库查询优化
* Velocity rules require counting recent transactions - how do you make this fast?
  频率规则需要统计近期交易数量，如何把这件事做快？
* Indexes on timestamp and account fields are crucial
  在时间戳和账户字段上建立索引至关重要。
* Consider using window functions or time-series databases
  可考虑窗口函数或时序数据库。
* SQL GROUP BY and HAVING clauses for aggregation
  使用 SQL 的 `GROUP BY` 和 `HAVING` 子句做聚合。

### Time-Based Logic
### 时间相关逻辑
* All velocity rules depend on accurate time handling
  所有频率规则都依赖准确的时间处理。
* Time zones matter!
  时区问题非常重要！
* Consider using UTC internally
  建议系统内部统一使用 UTC。
* Be careful with date/time arithmetic
  进行日期/时间运算时要特别谨慎。

## Project Presentations
## 项目展示

At the end of the program you will get the opportunity to present your project to your instructors and also potentially your manager and other interested stakeholders from within the firm.
在课程结束时，你们将有机会向讲师展示项目，也可能向经理及公司内其他相关方展示。

The duration of your presentation will be decided by your instructor, but they are typically 15-20 mins for groups of 3 and sometimes up to 25 or even 30 minutes for larger groups.
展示时长由讲师决定。通常 3 人小组为 15-20 分钟；更大组别有时可到 25 甚至 30 分钟。

### Presentation Guidelines
### 展示指引

Here is a suggested flow. You don't have to follow this exactly, but it gives you a suggested outline:
下面是建议流程。你们不必完全照做，但可作为展示大纲参考：

- Tell a story!
  讲一个完整的故事！
    - Your presentation should have a beginning, a middle and an end
      你的展示应有开头、中段和结尾。
- Start by introducing your team
  先介绍团队。
- Then introduce the project
  然后介绍项目。
    - What have you been learning?
      你们在学习什么？
    - What were you asked to do?
      你们被要求完成什么？
    - How much time have you had to work on it?
      你们有多少时间来做这个项目？
- Then explain how you approached the project
  接着说明你们如何推进这个项目。
    - Did you divide roles, e.g. backend or frontend?
      你们是否分工了，比如后端/前端？
    - Or did you code together, e.g. pair-programming?
      还是一起编码，比如结对编程？
    - What technologies and tools did you use?
      你们使用了哪些技术和工具？
- Then show what you built
  然后展示你们做出的成果。
    - Start with an overview of your data model – explain your decisions
      先概述数据模型，并解释设计决策。
    - Then show a high-level architecture of your application
      再展示应用的高层架构。
      - This could be a simple diagram in PowerPoint
        这可以是 PowerPoint 中的一张简图。
    - Demonstrate the rule engine in action with a live demo
      通过现场演示展示规则引擎如何运行。
    - Show how alerts are generated and managed
      展示告警如何生成和管理。
    - Demonstrate alert lifecycle (acknowledge, investigate, close)
      演示告警生命周期（确认、调查、关闭）。
- Then tell us what challenges you faced
  再说明你们遇到的挑战。
    - Did you work well together as a team?
      团队协作是否顺畅？
    - Were there any technical challenges?
      是否遇到技术难点？
    - What mistakes did you make?
      你们犯过哪些错误？
    - What would you do differently?
      如果重来，你们会做哪些不同选择？
    - How did you optimize rule evaluation performance?
      你们如何优化规则评估性能？
- Then tell us what you would do next if you had more time
  最后说明如果有更多时间，你们下一步会做什么。
- And finally – thank you for listening, any questions
  最后感谢聆听，并进入提问环节。

- Everyone is expected to speak
  每位成员都应发言。
- Keep your cameras on throughout the presentation
  展示全程保持摄像头开启。
- NOTE: YOU WILL BE EXPECTED TO ASK OTHER TEAMS QUESTIONS
  注意：你们将被要求向其他小组提问。

### Presentation Mechanics
### 展示安排细则

* Depending upon the size of your class, the presentations will be delivered with your groups nominated lead instructor
  具体展示安排会根据班级规模，由你们小组对应的主讲师组织。
* The lead instructor will typically have created a schedule for the presentations and will have circulated that in advance with 15-30 mins per group
  主讲师通常会提前制定并发布展示日程，通常每组 15-30 分钟。
* The presentation will NOT be allowed to overrun to ensure we keep to time
  为保证整体时间安排，展示不允许超时。
* The presentation schedule is sent out to wider firm staff so they know when to come if someone wants to see your presentation
  展示时间表会发给更广泛的公司同事，方便有兴趣的人按时参加。
* When a group says "any questions?", to avoid any unnecessary silences, the group that went before you MUST ask a question. If you are going first, then the group scheduled last must ask a question
  当某组说“有问题吗？”时，为避免冷场，上一组必须提一个问题；如果你们是第一组，则由最后一组提问。
* If your class is using virtual machines then they will continue to be available for the presentation
  如果你们班使用虚拟机，展示期间这些虚拟机仍会可用。

## Appendix A: Notes on Teamwork
## 附录 A：团队协作说明

It is expected that you work closely as a team during this project.
本项目期望你们以紧密协作的团队方式完成。

Your team should be self-organising, but should raise issues with instructors if they are potential blockers to progress.
团队应自组织推进；若出现可能阻碍进度的问题，应及时向讲师反馈。

Your team can use a task management system such as Trello to keep track of tasks and progress. Divide the work appropriately.
团队可以使用 Trello 等任务管理系统跟踪任务与进展，并合理分配工作。

Your team should keep track of all source code with git.
团队应使用 Git 管理全部源代码。

You may choose to create a separate repository for each component that you tackle e.g. front-end code can be in its own repository. If you create more than one back end application, then each can have its own repository. To keep track of your repositories, you can use a single 'Project' that each of your repositories is part of.
你们可以为不同组件建立独立仓库，例如前端代码单独一个仓库；若有多个后端应用，也可分别建仓库。为便于统一管理，可使用一个总的“Project”来纳管这些仓库。

Your instructor and team members need to access all repositories, so they should be either:
讲师和团队成员需要访问全部仓库，因此仓库应满足以下之一：

a) Made public
a）设为公开
b) Shared with your instructor and all team members.
b）共享给讲师和所有团队成员。

Throughout your work, you should ensure good communication and organise regular check-ins with each other.
在整个项目过程中，应保持良好沟通，并定期进行团队同步。

## Appendix C: UI Ideas
## 附录 C：UI 设计思路

Below are some UI concepts that might give you ideas. You are DEFINITELY NOT expected to implement these exactly as shown. This is JUST FOR DEMONSTRATION of the type of thing that COULD be shown.
下面给出一些 UI 概念供你们参考。你们绝对不需要按图一比一实现，这些仅用于演示“可以展示成什么样”。

### Alerts Dashboard Screen
### 告警看板页面
* Summary cards at top:
  顶部汇总卡片：
  - Open alerts count (red badge)
    未处理告警数量（红色徽标）
  - Acknowledged alerts count (yellow badge)
    已确认告警数量（黄色徽标）
  - Alerts today
    今日告警数
  - Average resolution time
    平均处理时长
* Alert list/table:
  告警列表/表格：
  - Columns: Alert ID, Severity, Rule Name, Status, Created Time
    列字段：告警 ID、严重级别、规则名称、状态、创建时间
  - Color coding by severity (red=HIGH, yellow=MEDIUM, blue=LOW)
    按严重级别配色（红=HIGH，黄=MEDIUM，蓝=LOW）
  - Filter by severity, status, date range
    支持按严重级别、状态、日期范围筛选
  - Click row to view details
    点击行可查看详情

### Alert Details Screen
### 告警详情页面
* Alert information:
  告警信息：
  - Alert ID and status with color badge
    告警 ID 与状态（带颜色徽标）
  - Rule that triggered it
    触发该告警的规则
  - Severity level
    严重级别
  - Created timestamp
    创建时间戳
  - Status history timeline
    状态历史时间线
* Related transactions section:
  关联交易区域：
  - Table of all transactions that triggered this alert
    显示触发该告警的全部交易列表
  - Transaction details (ID, amount, payee, timestamp)
    交易详情（ID、金额、收款方、时间戳）
* Actions section:
  操作区域：
  - "Acknowledge" button (if status = OPEN)
    “Acknowledge（确认）”按钮（当状态为 OPEN）
  - "Mark as Investigating" button
    “Mark as Investigating（标记为调查中）”按钮
  - "Close Alert" button with resolution notes text area
    “Close Alert（关闭告警）”按钮，并提供处理说明文本框
  - "Dismiss as False Positive" button
    “Dismiss as False Positive（按误报驳回）”按钮

### Transactions List Screen
### 交易列表页面
* Table of recent transactions:
  近期交易表格：
  - Columns: Transaction ID, Account, Payee, Amount, Timestamp, Status
    列字段：交易 ID、账户、收款方、金额、时间戳、状态
  - Search by transaction ID or description
    支持按交易 ID 或描述搜索
  - Filter by date range, account, amount range
    支持按日期范围、账户、金额区间筛选
  - Sort by any column
    支持任意列排序
  - Visual indicator if transaction triggered an alert
    若交易触发告警，提供可视化标识
* Summary statistics:
  汇总统计：
  - Total transaction count
    交易总数
  - Total transaction volume
    交易总金额
  - Alerts triggered
    触发告警数量

### Rules Management Screen
### 规则管理页面
* List of all rules:
  规则列表：
  - Table showing: Rule Name, Type, Status (Active/Inactive), Severity
    表格显示：规则名、类型、状态（启用/停用）、严重级别
  - Toggle to activate/deactivate rules
    通过开关启用/停用规则
  - Edit and Delete buttons
    编辑和删除按钮
* "Add New Rule" button opens a form:
  点击“Add New Rule（新增规则）”按钮后打开表单：
  - Rule name
    规则名称
  - Rule type (dropdown)
    规则类型（下拉框）
  - Severity level
    严重级别
  - Parameters based on rule type (dynamic form)
    根据规则类型动态展示参数（动态表单）
  - Active checkbox
    是否启用复选框

### Dashboard Charts (Advanced)
### 仪表板图表（进阶）
* Line chart: Transactions over time
  折线图：交易随时间变化
* Bar chart: Alerts by severity
  柱状图：按严重级别统计告警
* Pie chart: Alert status distribution
  饼图：告警状态分布
* Line chart: Alert response times trend
  折线图：告警响应时长趋势

## Appendix D: Advanced Features (If You Have Time)
## 附录 D：高级功能（如果你们有时间）

Once you have the core system working, consider these enhancements:
当核心系统可用后，可以考虑以下增强：

1. **Alert Grouping/Deduplication**
   **告警分组/去重**
   - Group similar alerts together
     将相似告警归为一组
   - "5 high-value transactions detected in the last hour"
     例如：“最近一小时检测到 5 笔高金额交易”
   - Reduce alert fatigue
     减少告警疲劳

2. **Machine Learning Integration**
   **机器学习集成**
   - Use historical data to identify anomalies
     使用历史数据识别异常
   - Learn "normal" patterns per account
     学习每个账户的“正常”行为模式
   - Flag unusual behavior
     标记异常行为

3. **Real-time Dashboard Updates**
   **实时看板更新**
   - WebSocket or Server-Sent Events
     使用 WebSocket 或 Server-Sent Events
   - Dashboard updates automatically when new alerts arrive
     新告警到达时看板自动更新
   - No need to refresh page
     无需手动刷新页面

4. **Alert Routing/Assignment**
   **告警路由/分派**
   - Assign alerts to specific operators
     将告警分配给指定操作员
   - Queue management
     队列管理
   - SLA tracking (time to acknowledge, time to resolve)
     SLA 跟踪（确认时长、解决时长）

5. **Rule Templates**
   **规则模板**
   - Pre-built rule templates for common scenarios
     为常见场景提供预置规则模板
   - Clone existing rules
     克隆现有规则
   - Import/export rule definitions
     导入/导出规则定义

6. **Reporting**
   **报表能力**
   - Daily/weekly/monthly alert summaries
     每日/每周/每月告警汇总
   - False positive rate tracking
     误报率跟踪
   - Operator performance metrics
     操作员绩效指标
   - Trend analysis
     趋势分析

7. **Notification System**
   **通知系统**
   - Email alerts for HIGH severity
     对 HIGH 级别告警发送邮件
   - SMS for critical alerts
     对关键告警发送短信
   - Webhook integration for external systems
     通过 Webhook 对接外部系统

8. **Audit Trail**
   **审计轨迹**
   - Track all changes to rules
     跟踪规则的所有变更
   - Track who acknowledged/closed each alert
     记录每条告警由谁确认/关闭
   - Compliance reporting
     合规报表

9. **Multi-Currency Support**
   **多币种支持**
   - Handle different currencies
     处理不同币种
   - Convert to base currency for thresholds
     将金额换算为基准币种后进行阈值判断
   - Currency-specific rules
     币种特定规则

10. **Complex Rule Logic**
    **复杂规则逻辑**
    - Combine multiple conditions with AND/OR logic
      使用 AND/OR 逻辑组合多个条件
    - "Amount > 10000 AND new payee"
      例如：“金额 > 10000 且为新收款方”
    - Time-of-day rules (alerts for transactions outside business hours)
      按时段规则（例如对营业时间外交易告警）

## Appendix E: Testing Considerations
## 附录 E：测试建议

Consider these testing scenarios:
可考虑以下测试场景：

1. **Rule Evaluation**
   **规则评估**
   - Submit transaction that exceeds amount threshold
     提交一笔超过金额阈值的交易
   - Verify alert is generated with correct severity
     验证是否生成了严重级别正确的告警
   - Submit transaction below threshold
     提交一笔低于阈值的交易
   - Verify no alert is generated
     验证不会生成告警

2. **Velocity Rules**
   **频率规则**
   - Submit 6 transactions in 5 minutes from same account
     在 5 分钟内从同一账户提交 6 笔交易
   - Verify velocity alert is triggered
     验证频率告警被触发
   - Wait 15 minutes and submit another transaction
     等待 15 分钟后再提交一笔交易
   - Verify no alert (outside time window)
     验证不触发告警（已超时间窗口）

3. **New Payee Detection**
   **新收款方检测**
   - Submit transaction to a payee never seen before
     提交一笔转给从未出现过收款方的交易
   - Verify new payee alert is generated
     验证会生成新收款方告警
   - Submit second transaction to same payee
     再向同一收款方提交第二笔交易
   - Verify no alert (payee already known)
     验证不会告警（收款方已被识别）

4. **Alert Lifecycle**
   **告警生命周期**
   - Create an alert
     创建一个告警
   - Acknowledge it and verify status change
     对告警执行确认并验证状态变化
   - Close it with notes and verify status change
     填写处理备注后关闭并验证状态变化
   - Verify all timestamps are recorded correctly
     验证所有时间戳都被正确记录

5. **Alert Status Validation**
   **告警状态校验**
   - Try to close an alert without acknowledging first
     尝试在未确认的情况下直接关闭告警
   - Decide if this should be allowed or not
     明确该行为是否允许
   - Try to reopen a closed alert
     尝试重新打开一个已关闭告警
   - Verify appropriate behavior
     验证系统行为是否符合预期

6. **Performance Testing**
   **性能测试**
   - Insert 1000 transactions rapidly
     快速插入 1000 笔交易
   - Measure rule evaluation time
     测量规则评估耗时
   - Ensure system remains responsive
     确保系统仍保持响应
   - Check database query performance
     检查数据库查询性能

7. **Concurrent Operations**
   **并发操作**
   - Two operators trying to acknowledge same alert simultaneously
     两个操作员同时确认同一条告警
   - Verify data consistency
     验证数据一致性

## Appendix F: Architecture Suggestions
## 附录 F：架构建议

Consider a layered architecture with asynchronous processing:
可考虑采用分层架构并引入异步处理：

```
┌─────────────────────────────────────┐
│         Web UI (Frontend)            │
│   (React, Vue, Angular, or similar)  │
└──────────────┬──────────────────────┘
               │ HTTP/REST
┌──────────────▼──────────────────────┐
│         REST API Layer               │
│    (Spring Boot / Flask / Express)   │
├──────────────────────────────────────┤
│      Transaction Service             │
│   - Record transactions              │
│   - Query transactions               │
├──────────────┬───────────────────────┤
│              │ Events/Messages       │
│              ▼                        │
│      Rule Engine Service             │
│   - Evaluate rules                   │
│   - Generate alerts                  │
├──────────────────────────────────────┤
│         Alert Service                │
│   - Manage alert lifecycle           │
│   - Alert queries and updates        │
├──────────────────────────────────────┤
│         Data Access Layer            │
│   - Repository pattern               │
│   - Transaction management           │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Database                     │
│   (PostgreSQL / MySQL / MongoDB)     │
└──────────────────────────────────────┘

Optional: Message Queue (RabbitMQ, Kafka)
for async transaction processing
```

**Key Considerations:**
**关键考量：**
* Separate transaction recording from rule evaluation for performance
  为提升性能，将交易记录与规则评估分离
* Consider asynchronous processing using message queues
  考虑用消息队列实现异步处理
* Make rule engine pluggable to easily add new rule types
  让规则引擎具备可插拔能力，便于扩展新规则类型
* Use caching for frequently accessed data (e.g., historical transaction counts)
  对高频访问数据使用缓存（如历史交易计数）
* Implement proper indexing on timestamp and account fields
  在时间戳和账户字段上建立合适索引
* Consider read replicas for reporting queries
  报表查询可考虑读副本（read replicas）
* Use transaction isolation appropriately
  合理使用事务隔离级别

## Appendix G: Rule Engine Implementation Patterns
## 附录 G：规则引擎实现模式

### Strategy Pattern Approach
### 策略模式方案

Each rule type is a separate class implementing a common interface:
每种规则类型都由独立类实现同一个通用接口：

```
interface Rule {
  evaluate(transaction, context): Alert[]
}

class AmountThresholdRule implements Rule {
  evaluate(transaction, context) {
    if (transaction.amount > this.threshold) {
      return createAlert(transaction, this);
    }
    return [];
  }
}

class VelocityRule implements Rule {
  evaluate(transaction, context) {
    recentCount = context.countRecentTransactions(
      transaction.accountId, 
      this.timeWindow
    );
    if (recentCount > this.maxTransactions) {
      return createAlert(transaction, this);
    }
    return [];
  }
}

// Rule engine iterates through all active rules
for each rule in activeRules:
  alerts = rule.evaluate(transaction, context)
  if alerts:
    save(alerts)
```

### Configuration-Driven Approach
### 配置驱动方案

Rules are data, not code:
规则是数据，而不是硬编码逻辑：

```
rules = loadFromDatabase()
for each rule in rules:
  result = evaluateRule(rule, transaction)
  if result.triggered:
    createAlert(rule, transaction, result)
```

This approach makes rules configurable without code changes but may be less flexible for complex logic.
这种方式让规则无需改代码即可配置，但面对复杂逻辑时灵活性可能稍弱。

## Appendix H: Sample Test Data Generator
## 附录 H：测试数据生成器示例

To test your system, you may want to create a test data generator:
为了测试系统，你可能需要一个测试数据生成器：

### Random Transaction Generator (Pseudocode)
### 随机交易生成器（伪代码）

```
function generateTestTransactions(count):
  accounts = ["ACC-001", "ACC-002", "ACC-003"]
  payees = ["PAYEE-A", "PAYEE-B", "PAYEE-C", "PAYEE-NEW"]
  
  for i in 1 to count:
    transaction = {
      accountId: random(accounts),
      payeeId: random(payees),
      amount: randomAmount(10, 20000),
      currency: "USD",
      type: "DEBIT",
      timestamp: now(),
      description: "Test transaction " + i
    }
    postTransaction(transaction)
    sleep(randomMillis(100, 1000))
```

### Scenario-Based Test Generator
### 场景化测试生成器

Create specific scenarios to trigger alerts:
构造特定场景以触发告警：

```
// Scenario 1: High-value transaction
postTransaction(account="ACC-001", amount=15000)

// Scenario 2: Velocity - rapid transactions
for i in 1 to 10:
  postTransaction(account="ACC-002", amount=100)
  sleep(30 seconds)

// Scenario 3: New payee
postTransaction(account="ACC-003", payee="PAYEE-BRAND-NEW", amount=5000)
```
