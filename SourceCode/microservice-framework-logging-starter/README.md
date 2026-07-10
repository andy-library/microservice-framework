# Microservice Framework Logging Starter

Author: Andy Yang

为微服务应用提供**高性能、结构化、安全**的企业级日志解决方案，与 Observability Starter 深度集成。

## 组件定位

`microservice-framework-logging-starter` 是 Microservice Framework 的**日志核心组件**，提供：
- **结构化日志**：JSON 格式输出，便于 ELK/Loki 等日志系统采集
- **可观测性集成**：自动关联 TraceId/SpanId/RequestId
- **安全保护**：敏感数据自动脱敏
- **稳定性保障**：日志风暴防护

> **重要**：本组件**传递性依赖** `microservice-framework-observability-starter`，自动获得全部可观测性能力。

## 核心能力

### 1. JSON 结构化日志
```json
{
  "@timestamp": "2025-12-24T16:33:55.915+08:00",
  "level": "INFO",
  "message": "用户登录成功",
  "traceId": "79fbfb9e0eacfb88",
  "spanId": "47f09929bf0b6d2b",
  "requestId": "79fbfb9e0eacfb88",
  "userId": "12345",
  "app": "microservice-crm-service"
}
```

### 2. 高性能异步日志
- 基于 Logback `AsyncAppender`
- 可配置队列大小和丢弃策略
- 支持优雅停机（等待队列清空）

### 3. 敏感数据脱敏
| 类型 | 原始值 | 脱敏后 |
|------|--------|--------|
| 手机号 | `13812345678` | `138****5678` |
| 身份证 | `110101199001011234` | `110101********1234` |
| 银行卡 | `6222021234567890123` | `6222************0123` |
| 邮箱 | `test@example.com` | `te***@example.com` |

### 4. 日志风暴防护
- 令牌桶限流算法
- 可配置速率和突发容量
- 防止日志洪水压垮系统

### 5. 动态日志级别
通过 Actuator 端点运行时调整日志级别，无需重启。

## 使用场景

### 场景 1：结构化日志记录
```java
import com.microservice.framework.logging.util.LogUtils;

// 带键值对的结构化日志
LogUtils.info("订单创建成功",
    LogUtils.kv("orderId", "ORD123"),
    LogUtils.kv("amount", 99.99),
    LogUtils.kv("userId", "U001"));
```

### 场景 2：异常日志
```java
try {
    // 业务逻辑
} catch (Exception e) {
    LogUtils.error("支付失败", e,
        LogUtils.kv("orderId", orderId),
        LogUtils.kv("errorCode", "PAY_001"));
}
```

### 场景 3：敏感信息记录
```java
// 敏感信息会被自动脱敏（当 masking.enabled=true）
LogUtils.info("用户注册",
    LogUtils.kv("phone", "13812345678"),      // -> 138****5678
    LogUtils.kv("idCard", "110101199001011234")); // -> 110101********1234
```

## 快速开始

### 1. 添加依赖
```xml
<dependency>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-logging-starter</artifactId>
</dependency>
```

> 注意：无需单独添加 `observability-starter`，已作为传递性依赖包含。

### 2. 配置 application.yml

#### 完整配置示例（含所有参数说明）
```yaml
# ============================================================================
# Microservice Framework Logging 配置
# ============================================================================
framework:
  logging:
    # ------------------------------------------------------------------------
    # 异步日志配置 (Async)
    # 使用 Logback AsyncAppender 实现非阻塞日志写入
    # ------------------------------------------------------------------------
    async:
      # 是否启用异步日志
      # 类型: boolean | 默认值: true
      # 生产环境建议开启，开发环境可关闭便于调试
      enabled: true
      
      # 异步队列大小
      # 类型: int | 默认值: 256
      # 建议值：
      #   - 低流量服务: 256
      #   - 中流量服务: 512
      #   - 高流量服务: 1024
      # 注意：过大会占用更多内存，过小可能导致日志丢失
      queue-size: 256
      
      # 丢弃阈值
      # 类型: int | 默认值: 0 (不丢弃)
      # 当队列剩余容量低于此值时，开始丢弃低优先级日志(TRACE, DEBUG)
      # 设为 0 表示永不丢弃
      # 生产环境建议设为 queue-size 的 10% (如 queue-size=256 时设为 25)
      discarding-threshold: 0
    
    # ------------------------------------------------------------------------
    # 日志风暴防护配置 (Flood Protection)
    # 使用令牌桶算法限制日志输出速率，防止日志洪水
    # ------------------------------------------------------------------------
    flood-protection:
      # 是否启用限流
      # 类型: boolean | 默认值: true
      # 生产环境强烈建议开启
      enabled: true
      
      # 每秒允许的日志数量 (令牌生成速率)
      # 类型: int | 默认值: 10
      # 建议值：
      #   - 开发环境: 100 或关闭
      #   - 测试环境: 50
      #   - 生产环境: 10-20
      rate: 10
      
      # 突发容量 (令牌桶容量)
      # 类型: int | 默认值: 100
      # 允许瞬时突发的最大日志数量
      # 应用启动时会有一次性大量日志，此值应足够容纳
      burst-capacity: 100
    
    # ------------------------------------------------------------------------
    # 数据脱敏配置 (Masking)
    # 自动识别并脱敏日志中的敏感信息
    # ------------------------------------------------------------------------
    masking:
      # 是否启用脱敏
      # 类型: boolean | 默认值: false
      # 生产环境必须开启！
      enabled: true
      
      # 启用的默认规则
      # 类型: List<String> | 默认值: [MOBILE_PHONE, ID_CARD]
      # 可选规则：
      #   - MOBILE_PHONE: 手机号 (138****5678)
      #   - ID_CARD: 身份证号 (110101********1234)
      #   - BANK_CARD: 银行卡号 (6222************0123)
      #   - EMAIL: 邮箱 (te***@example.com)
      enabled-default-rules:
        - MOBILE_PHONE
        - ID_CARD
        - BANK_CARD
        - EMAIL
      
      # 自定义脱敏规则
      # 类型: List<CustomMaskingRule>
      # 用于添加业务特定的脱敏规则
      custom-rules:
        # 示例：脱敏 token 参数
        - name: ACCESS_TOKEN       # 规则名称（用于日志和调试）
          regex: "token=([^&]+)"   # 匹配的正则表达式
          mask: "token=***"        # 替换为的脱敏值
        # 示例：脱敏密码字段
        - name: PASSWORD
          regex: "\"password\":\"[^\"]+\""
          mask: "\"password\":\"******\""
    
    # ------------------------------------------------------------------------
    # Trace 关联采样配置 (Trace Sampling)
    # 根据 Trace 采样状态调整日志级别，减少未采样请求的日志量
    # ------------------------------------------------------------------------
    trace-sampling:
      # 是否启用 Trace 关联采样
      # 类型: boolean | 默认值: true
      # 启用后，未被采样的请求只记录指定级别以上的日志
      enabled: true
      
      # 未采样 Trace 的日志级别阈值
      # 类型: String | 默认值: "INFO"
      # 可选值: TRACE, DEBUG, INFO, WARN, ERROR
      # 未采样的请求只记录此级别及以上的日志
      # 建议：
      #   - 开发环境: DEBUG (查看所有日志)
      #   - 测试环境: INFO
      #   - 生产环境: WARN (大幅减少日志量)
      level-for-unsampled: INFO

# ============================================================================
# Spring Boot Logging 配置
# ============================================================================
logging:
  level:
    # 根日志级别
    root: INFO
    # 业务代码日志级别
    com.microservice: INFO
    # Spring 框架日志级别
    org.springframework: WARN
    # Hibernate SQL 日志（按需开启）
    org.hibernate.SQL: WARN
```

### 3. 使用 LogUtils
```java
import com.microservice.framework.logging.util.LogUtils;

LogUtils.info("操作成功", LogUtils.kv("key", "value"));
LogUtils.warn("警告信息", LogUtils.kv("code", "W001"));
LogUtils.error("错误信息", exception, LogUtils.kv("errorId", "E001"));
```

## 环境配置最佳实践

### 开发环境
```yaml
framework:
  logging:
    async:
      enabled: false          # 同步日志，便于调试时立即看到输出
    masking:
      enabled: false          # 关闭脱敏，便于排查问题
    flood-protection:
      enabled: false          # 关闭限流，查看完整日志
    trace-sampling:
      level-for-unsampled: DEBUG  # 查看所有日志

logging:
  level:
    root: INFO
    com.microservice: DEBUG           # 开启 DEBUG 级别
```

### 测试环境
```yaml
framework:
  logging:
    async:
      enabled: true
      queue-size: 256
    masking:
      enabled: true           # 开启脱敏验证
      enabled-default-rules:
        - MOBILE_PHONE
        - ID_CARD
    flood-protection:
      enabled: true
      rate: 50                # 放宽限流，便于测试
      burst-capacity: 200
    trace-sampling:
      enabled: true
      level-for-unsampled: INFO

logging:
  level:
    root: INFO
    com.microservice: DEBUG
```

### 生产环境
```yaml
framework:
  logging:
    async:
      enabled: true
      queue-size: 512         # 增大队列应对高流量
      discarding-threshold: 50  # 队列满时丢弃低优先级日志
    masking:
      enabled: true           # 必须开启！保护用户隐私
      enabled-default-rules:
        - MOBILE_PHONE
        - ID_CARD
        - BANK_CARD
        - EMAIL
    flood-protection:
      enabled: true
      rate: 10                # 严格限流
      burst-capacity: 100
    trace-sampling:
      enabled: true
      level-for-unsampled: WARN  # 未采样请求只记录 WARN+

logging:
  level:
    root: INFO
    com.microservice: INFO
    org.springframework: WARN
    org.hibernate: WARN

## API 参考

### LogUtils 工具类
```java
// 信息日志
LogUtils.info("message", LogUtils.kv("key", "value"));

// 警告日志
LogUtils.warn("message", LogUtils.kv("key", "value"));

// 错误日志（带异常）
LogUtils.error("message", exception, LogUtils.kv("key", "value"));

// 创建键值对
Object kv = LogUtils.kv("key", "value");
```

### Actuator 端点

#### 查看日志级别
```bash
curl http://localhost:8080/actuator/loggers/com.microservice
```

#### 动态调整日志级别
```bash
curl -X POST http://localhost:8080/actuator/loggers/com.microservice \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'
```

## 配置参考

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `framework.logging.async.enabled` | `true` | 启用异步日志 |
| `framework.logging.async.queue-size` | `256` | 异步队列大小 |
| `framework.logging.async.discarding-threshold` | `0` | 丢弃阈值（0=不丢弃） |
| `framework.logging.flood-protection.enabled` | `true` | 启用日志限流 |
| `framework.logging.flood-protection.rate` | `10` | 每秒允许日志数 |
| `framework.logging.flood-protection.burst-capacity` | `100` | 突发容量 |
| `framework.logging.masking.enabled` | `false` | 启用数据脱敏 |
| `framework.logging.trace-sampling.enabled` | `true` | 启用 Trace 关联采样 |
| `framework.logging.trace-sampling.level-for-unsampled` | `INFO` | 未采样请求的日志级别阈值 |

## 常见问题

### Q: 日志中没有 TraceId？
确保已启用 `observability-starter` 的网关集成：
```yaml
framework:
  observability:
    gateway-integration:
      enabled: true
```

### Q: 如何自定义脱敏规则？
```yaml
framework:
  logging:
    masking:
      enabled: true
      custom-rules:
        - name: CUSTOM_FIELD
          regex: "secret=([^&]+)"
          mask: "secret=***"
```

### Q: 日志输出到文件？
在 `logback-spring.xml` 中配置 `FileAppender`。

## 相关文档

- [Observability Starter](../microservice-framework-observability-starter/README.md)
- [Framework Demo](../microservice-framework-demo/README.md)
