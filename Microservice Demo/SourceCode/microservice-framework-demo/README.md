# Microservice Framework Demo

Author: Andy Yang

演示项目，用于验证 Microservice Framework 的所有能力。

## 功能覆盖

### 1. Observability Starter (可观测性)
- ✅ 分布式追踪（Micrometer Tracing + OpenTelemetry）
- ✅ 自动 MDC 注入（traceId, spanId）
- ✅ Baggage 传播
- ✅ @SpanTag 注解
- ✅ 动态采样率配置
- ✅ Metrics 指标收集
- ✅ Prometheus 端点
- ✅ **网关集成** (Request ID 三级降级：TraceId → Header → UUID)

### 2. Logging Starter (日志)
- ✅ JSON 结构化日志
- ✅ **Request ID 自动注入** (由 GatewayIntegrationFilter 管理)
- ✅ 异步日志 + 优雅停机
- ✅ LogUtils 工具类
- ✅ 日志风暴防护（令牌桶限流）
- ✅ 数据脱敏（手机号、身份证、邮箱、银行卡）
- ✅ Trace 关联采样
- ✅ 动态日志级别调整

## 快速开始

### 1. 构建项目

```bash
cd /Volumes/Development\ HD/AI\ PRD/AI-PRD/Framework/microservice-framework-demo
mvn clean install
```

### 2. 运行应用

```bash
mvn spring-boot:run
```

或在 IntelliJ IDEA 中直接运行 `DemoApplication`。

应用将在 `http://localhost:8080` 启动。

### 3. 验证启动

访问健康检查端点：
```bash
curl http://localhost:8080/actuator/health
```

## API 测试

### 方式 1：使用 Postman

1. 导入 Postman Collection：`postman/Microservice-Framework-Demo.postman_collection.json`
2. 按照分组顺序测试各个功能

### 方式 2：使用 curl

#### 创建用户（测试数据脱敏）
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john.doe@example.com",
    "phone": "13812345678",
    "idCard": "110101199001011234",
    "bankCard": "6222021234567890123"
  }'
```

**查看控制台日志**，敏感信息会被自动脱敏：
- 手机号：`138****5678`
- 身份证：`110101********1234`
- 邮箱：`jo****@example.com`
- 银行卡：`6222************0123`

#### 测试日志限流
```bash
curl http://localhost:8080/api/demo/logging/flood
```

查看控制台，虽然尝试记录 1000 条日志，但由于限流（rate=10, burst=100），只会输出约 100 条。

#### 测试追踪
```bash
curl http://localhost:8080/api/demo/tracing/current
```

返回当前请求的 traceId 和 spanId。

#### 测试 Baggage 传播
```bash
curl -H "user-id: user123" \
     -H "tenant-id: tenant456" \
     http://localhost:8080/api/demo/tracing/baggage
```

#### 测试 Request ID 三级降级
```bash
# 场景1: 验证 TraceId 优先 (Trace 自动创建)
curl http://localhost:8080/api/demo/request-id

# 场景2: 模拟网关透传 Header (TraceId 仍会覆盖)
curl -H "X-Request-ID: my-gateway-id" http://localhost:8080/api/demo/request-id/gateway-test

# 场景3: 获取完整上下文信息
curl http://localhost:8080/api/demo/request-id/full-context
```

### 方式 3：运行 JUnit 测试

```bash
mvn test
```

测试覆盖：
- `LoggingIntegrationTest` - 日志功能测试
- `TracingIntegrationTest` - 追踪功能测试

## Actuator 端点

| 端点 | 说明 |
|------|------|
| `/actuator/health` | 健康检查 |
| `/actuator/info` | 应用信息 |
| `/actuator/metrics` | 指标列表 |
### 5. 定时任务追踪 (Scheduled Tasks)
演示非 HTTP 入口（如后台任务）的自动追踪能力。

- **功能**: 每 60 秒自动执行一次模拟报表任务
- **验证**:
  - 观察控制台日志，寻找 `job:daily-report` 相关的日志
  - 确认日志中包含 `traceId` 和 `spanId`
  - 确认 `taskId` 在任务生命周期内的一致性

### 6. 自定义业务指标 (Custom Metrics)
演示如何使用 Micrometer 记录业务指标。

- **计数器 (Counter)**:
  - 请求: `GET /api/demo/metrics/counter?channel=app`
  - 验证: 访问 `/actuator/prometheus` 搜索 `business_order_created_total`
- **计时器 (Timer)**:
  - 请求: `GET /api/demo/metrics/timer`
  - 验证: 访问 `/actuator/prometheus` 搜索 `business_payment_process_seconds`
| `/actuator/prometheus` | Prometheus 格式指标 |
| `/actuator/loggers` | 日志级别查看/调整 |

### 动态调整日志级别

查看当前日志级别：
```bash
curl http://localhost:8080/actuator/loggers/com.microservice.demo
```

调整为 DEBUG 级别：
```bash
curl -X POST http://localhost:8080/actuator/loggers/com.microservice.demo \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'
```

## 项目结构

```
microservice-framework-demo/
├── src/main/java/com/microservice/demo/
│   ├── DemoApplication.java           # 主启动类
│   ├── controller/
│   │   ├── UserController.java        # 用户 API
│   │   ├── OrderController.java       # 订单 API
│   │   ├── LogDemoController.java     # 日志演示
│   │   ├── TracingDemoController.java # 追踪演示
│   │   └── HealthDemoController.java  # 健康检查
│   ├── service/
│   │   ├── UserService.java           # 用户服务
│   │   └── OrderService.java          # 订单服务
│   └── model/
│       ├── User.java                  # 用户实体
│       └── Order.java                 # 订单实体
├── src/main/resources/
│   └── application.yml                # 配置文件
├── src/test/java/
│   ├── LoggingIntegrationTest.java    # 日志测试
│   └── TracingIntegrationTest.java    # 追踪测试
└── postman/
    └── Microservice-Framework-Demo.postman_collection.json
```

## 测试场景

### 场景 1：数据脱敏验证
1. 创建用户（包含敏感信息）
2. 查看控制台日志
3. 验证敏感信息已被脱敏

### 场景 2：日志限流验证
1. 调用 `/api/demo/logging/flood`
2. 查看控制台日志
3. 验证只输出约 100 条日志（burst capacity）

### 场景 3：分布式追踪验证
1. 创建用户
2. 创建订单（会调用 UserService）
3. 查看日志中的 traceId
4. 验证同一请求的所有日志具有相同的 traceId

### 场景 4：异常日志验证
1. 调用 `/api/users/{userId}/error`
2. 查看日志中的异常字段：
   - `error.class`
   - `error.message`
   - `error.stack_hash`

### 场景 5：动态日志级别
1. 调整日志级别为 DEBUG
2. 调用任意 API
3. 查看控制台输出 DEBUG 日志
4. 恢复为 INFO 级别

## 配置说明

关键配置在 `application.yml`：

```yaml
framework:
  logging:
    async:
      enabled: true              # 异步日志
    flood-protection:
      enabled: true              # 日志限流
      rate: 10                   # 每秒 10 条
      burst-capacity: 100        # 突发容量 100
    masking:
      enabled: true              # 数据脱敏
    trace-sampling:
      enabled: true              # Trace 采样
  
  observability:
    tracing:
      sampling-probability: 1.0  # 100% 采样
    baggage:
      enabled: true              # Baggage 传播
```

## 注意事项

1. **数据脱敏**：默认开启，会自动脱敏日志中的敏感信息
2. **日志限流**：默认 rate=10/s, burst=100，可根据需要调整
3. **Trace 采样**：演示环境设置为 100%，生产环境建议降低
4. **内存存储**：用户和订单数据存储在内存中，重启后丢失

## 下一步

- 集成 Zipkin/Jaeger 可视化追踪链路
- 集成 Grafana 可视化指标
- 添加更多业务场景
- 创建第二个服务演示跨服务调用

## 问题排查

### 日志未脱敏
检查 `framework.logging.masking.enabled` 是否为 `true`

### 日志限流未生效
检查 `framework.logging.flood-protection.enabled` 是否为 `true`

### Trace ID 为空
确保请求经过 Spring MVC，Trace 会自动创建

## 许可证

MIT License
