# Microservice Framework Kafka Starter

Author: Andy Yang

Kafka Starter — 提供 Kafka 生产、消费、提交、幂等、重试和死信抽象。

## 核心能力

| 能力 | API 接口 | 说明 |
| --- | --- | --- |
| 消息发布 | `KafkaPublisher` | 委托 KafkaTemplate 实现消息发送，支持单条、批量、带 key 和指定 topic |
| 消费者构建 | `KafkaConsumerBuilder` | Builder 模式构建消费者配置，支持 topic、groupId、并发度等参数 |
| 死信处理 | `DeadLetterHandler` | 消费失败超过重试次数后转发到死信主题，可自定义持久化或告警 |
| 幂等过滤 | `IdempotencyFilter` | 基于 topic-partition-offset 去重，默认使用 ConcurrentHashMap（单实例），生产环境应覆盖为 Redis 实现 |

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-kafka-starter</artifactId>
</dependency>
```

版本由 `microservice-framework-bom` 统一管理，无需指定。

### 2. 最小配置

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

framework:
  kafka:
    producer:
      acks: all               # 确认级别
      retries: 3              # 重试次数
      batch-size: 16384       # 批量发送大小（字节）
      linger-ms: 0            # 批量延迟等待时间（毫秒）
    consumer:
      auto-commit: false      # 手动提交
      concurrency: 3          # 消费者并发数
      auto-commit-interval-ms: 1000
    dead-letter:
      enabled: true           # 是否启用死信队列
      max-retries: 3          # 最大重试次数
      topic-suffix: ".DLT"    # 死信主题后缀
    idempotency:
      enabled: true           # 是否启用幂等消费过滤
```

### 3. 使用示例

```java
@Autowired
private KafkaPublisher<Object> kafkaPublisher;

public void sendEvent(OrderEvent event) {
    kafkaPublisher.publishToTopic("order-events", event)
        .thenAccept(metadata -> log.info("Sent to partition {}", metadata.partition()));
}
```

## 配置参考

所有配置前缀为 `framework.kafka`。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用 Kafka Starter |
| `producer.acks` | `all` | 确认级别 |
| `producer.retries` | `3` | 重试次数 |
| `producer.batch-size` | `16384` | 批量发送大小（字节） |
| `producer.linger-ms` | `0` | 批量延迟等待时间（毫秒） |
| `consumer.auto-commit` | `false` | 是否自动提交 |
| `consumer.concurrency` | `3` | 消费者并发数 |
| `consumer.auto-commit-interval-ms` | `1000` | 自动提交间隔（毫秒） |
| `dead-letter.enabled` | `true` | 是否启用死信队列 |
| `dead-letter.max-retries` | `3` | 最大重试次数 |
| `dead-letter.topic-suffix` | `.DLT` | 死信主题后缀 |
| `idempotency.enabled` | `true` | 是否启用幂等消费过滤 |

## 自动注册 Bean

当 `framework.kafka.enabled=true` 时，自动注册：

| Bean 名称 | 类型 | 条件 |
| --- | --- | --- |
| `kafkaPublisher` | `KafkaPublisher<Object>` | `@ConditionalOnBean(KafkaTemplate)` + `@ConditionalOnMissingBean` |
| `kafkaConsumerBuilder` | `KafkaConsumerBuilder` | `@ConditionalOnMissingBean` |
| `deadLetterHandler` | `DeadLetterHandler<Object, Object>` | `@ConditionalOnMissingBean` + `framework.kafka.dead-letter.enabled=true` |
| `idempotencyFilter` | `IdempotencyFilter<Object, Object>` | `@ConditionalOnMissingBean` + `framework.kafka.idempotency.enabled=true` |

用户可通过注册自定义同类型 Bean 覆盖默认实现。

## 集成测试策略

| 状态 | 说明 |
| --- | --- |
| 当前覆盖 | 自动配置条件、属性绑定、API 契约（Embedded Kafka Broker via spring-kafka-test） |
| 真实中间件 | 暂未包含 |
| 计划方案 | Testcontainers Kafka 模块提供真实 Kafka Broker，验证发布-消费全链路、死信转发、幂等去重等需要真实 Broker 行为的场景 |
| 执行方式 | Failsafe + `@Tag("integration")` profile，不在默认 `mvn clean verify` 中 |
| 补齐时间 | 下一迭代 |

当前 `mvn clean verify` 仍为默认准入门禁。真实中间件集成测试将在 Dependencies BOM 补齐对应 Testcontainers 模块后落地。
