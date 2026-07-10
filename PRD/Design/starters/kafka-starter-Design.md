# Kafka Starter 技术与测试设计

Author: Andy Yang

对应需求：[Kafka PRD](../../PRD/starters/kafka-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Spring for Apache Kafka、Kafka Client；版本沿用 Spring Boot BOM。Observability 为 optional。

## 2. 技术设计

- 自动配置：Producer、ManualConsumer、FrameworkManagedConsumer、Retry/DLT、Metrics。
- 公共 API：`FrameworkMessage<T>`、`MessageHeaders`、`MessageProducer`、`ManualCommitConsumer`、`ManagedCommitConsumer`、`IdempotencyGuard`。
- 默认关闭 Kafka 自动提交；手动策略仅在业务确认后 ack；框架策略仅在无异常完成后提交。
- 重试 Topic 与 DLT 命名、次数、退避统一；错误分类决定直接死信或重试。
- 消费线程由 Kafka 管理，业务并发通过容器参数治理，不复用 Async。
- 消息载荷默认 JSON，Schema 扩展通过 SPI；缓存更新事件只携带标识与事实。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 自动配置 | 两种消费策略、默认手动提交、非法并发/提交配置 |
| 集成 | Testcontainers Kafka；生产、消费、Header、批量、顺序 |
| 可靠性 | 处理前/后崩溃、重复消息、重试、DLT、Broker 恢复 |
| 背压 | 慢消费者、积压、暂停/恢复和优雅停机 |
| 契约 | 两种消费者基类的提交时点与异常语义 |
| 性能 | 生产/消费吞吐、批量大小和序列化基准 |

通过条件：失败消息不提前提交；重复消息不产生重复业务结果；过载有界。
