# Starter PRD 索引

Author: Andy Yang

组件目录已经冻结。每个 Starter 必须在对应 PRD 约束下完成技术设计、开发和验收。

| 顺序 | PRD |
| ---: | --- |
| 1 | [Common Starter](./common-starter-PRD.md) |
| 2 | [JSON Starter](./json-starter-PRD.md) |
| 3 | [Logging Starter](./logging-starter-PRD.md) |
| 4 | [Nacos Starter](./nacos-starter-PRD.md) |
| 5 | [Apollo Starter](./apollo-starter-PRD.md) |
| 6 | [Observability Starter](./observability-starter-PRD.md) |
| 7 | [Database Starter](./database-starter-PRD.md) |
| 8 | [Redis Starter](./redis-starter-PRD.md) |
| 9 | [Kafka Starter](./kafka-starter-PRD.md) |
| 10 | [Elasticsearch Starter](./elasticsearch-starter-PRD.md) |
| 11 | [Async Starter](./async-starter-PRD.md) |
| 12 | [XXL-JOB Starter](./xxl-job-starter-PRD.md) |
| 13 | [Web Starter](./web-starter-PRD.md) |
| 14 | [Feign Starter](./feign-starter-PRD.md) |
| 15 | [Security Starter](./security-starter-PRD.md) |
| 16 | [Drools Starter](./drools-starter-PRD.md) |
| 17 | [Audit Starter](./audit-starter-PRD.md) |
| 18 | [Field Encryption Starter](./field-encryption-starter-PRD.md) |
| 19 | [Object Storage Starter](./object-storage-starter-PRD.md) |

## 统一验收门禁

每个 Starter 均必须通过：

1. 独立 `mvn clean verify`。
2. 自动配置启用、禁用、非法配置、缺失依赖和用户覆盖测试。
3. 依赖树边界检查。
4. 最小真实应用启动测试。
5. 与所有前序 Starter 的集成回归。
6. README、配置元数据、示例和限制检查。

## 单份 Starter PRD 必备内容

每份 Starter PRD 必须明确：

1. 定位、适用范围与不负责的边界。
2. 依赖关系、技术栈和可选组合。
3. 核心能力及默认行为。
4. 实现约束、失败语义、安全与性能要求。
5. 配置前缀、扩展接口和禁止项。
6. 可执行验收场景、方法与精确通过条件。
7. 开发完成时必须交付的源码、文档、测试和验证应用。

对应技术设计与测试设计位于 [`Design/starters`](../../Design/starters/)。开发必须同时满足 PRD、组件设计和统一工程基线。
