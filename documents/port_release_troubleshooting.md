# 后端停止后端口仍被占用的排查说明

## 1. 现象

停止 Spring Boot 后端后，`8080` 端口仍然被占用，下一次启动前需要先手动结束进程。

## 2. 当前项目结论

已检查当前后端代码：

- `backend/src/main/java/com/example/monitoring/TransactionMonitoringApplication.java` 只有标准 `SpringApplication.run(...)`
- 业务代码中未发现以下常见“阻止 JVM 退出”的后台线程写法：
  - `new Thread(...)`
  - `ExecutorService`
  - `@Async`
  - `@Scheduled`
  - `CompletableFuture`

因此，这个问题在当前项目里**更像是运行方式 / IDE 停止方式 / JDK 与 Maven 运行器配置**导致，而不是业务代码里手写线程没有关闭。

## 3. 已做的配置修复

已在 `backend/src/main/resources/application.properties` 增加：

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=20s
spring.main.register-shutdown-hook=true
```

作用：

- `server.shutdown=graceful`：优雅关闭 Web 服务
- `spring.lifecycle.timeout-per-shutdown-phase=20s`：给 Spring Bean 关闭留出时间
- `spring.main.register-shutdown-hook=true`：JVM 退出时自动触发 Spring 上下文关闭

## 4. 最常见根因与解决方式

### 4.1 在 JetBrains 中没有真正停止进程
请确认你点击的是：

- **Stop**（红色方块）

而不是：

- 只是关闭 Run 窗口
- 重新运行导致旧进程未完全退出

### 4.2 IDE 的运行器和终端用的不是同一个 JDK
建议统一：

- Project SDK
- Module SDK
- Maven Runner JDK

如果 `pom.xml` 是 Java 17，就都统一成 17。

### 4.3 使用了 Maven 方式启动，但 IDE 只停掉了前台任务
如果你是通过下面方式启动：

```powershell
mvn spring-boot:run
```

请用同一个终端按：

```powershell
Ctrl + C
```

不要直接关终端窗口。

## 5. 推荐启动方式

### 方式 A：直接运行主类
推荐直接运行：

- `TransactionMonitoringApplication`

这样通常比 `mvn spring-boot:run` 更稳定，停止时也更容易释放端口。

### 方式 B：终端启动并用 Ctrl+C 停止

```powershell
Set-Location "C:\Users\Administrator\Desktop\RAC-awesomest-team\backend"
mvn spring-boot:run
```

停止时：

```powershell
Ctrl + C
```

## 6. 如果端口仍被占用，如何快速查进程

查看 8080 端口：

```powershell
netstat -ano | findstr :8080
```

查看 PID 对应进程：

```powershell
Get-Process -Id <PID>
```

结束进程：

```powershell
Stop-Process -Id <PID> -Force
```

## 7. 推荐排查顺序

1. 先确认是不是正确停止了运行中的 Spring Boot 进程
2. 再确认 JetBrains 的 Project SDK / Maven Runner JDK 是否一致
3. 再观察终端停止时是否打印 Spring 的关闭日志
4. 如果仍有问题，再继续排查是否存在外部工具持有进程（如热部署、重复运行配置）

