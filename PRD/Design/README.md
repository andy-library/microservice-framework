# Microservice Framework 设计文档索引

Author: Andy Yang

本目录是已冻结 PRD 进入开发前的技术设计与测试设计基线。实现必须同时满足对应 PRD、统一工程基线和组件设计。

## 统一设计

- [统一工程与测试基线](./Engineering-Baseline.md)
- [组件技术栈矩阵](./Technology-Stack-Matrix.md)
- [Parent 设计](./parent-Design.md)
- [集成测试策略](./Integration-Test-Strategy.md)

## Starter 设计

| 顺序 | 设计文档 |
| ---: | --- |
| 1 | [Common](./starters/common-starter-Design.md) |
| 2 | [JSON](./starters/json-starter-Design.md) |
| 3 | [Logging](./starters/logging-starter-Design.md) |
| 4 | [Nacos](./starters/nacos-starter-Design.md) |
| 5 | [Apollo](./starters/apollo-starter-Design.md) |
| 6 | [Observability](./starters/observability-starter-Design.md) |
| 7 | [Database](./starters/database-starter-Design.md) |
| 8 | [Redis](./starters/redis-starter-Design.md) |
| 9 | [Kafka](./starters/kafka-starter-Design.md) |
| 10 | [Elasticsearch](./starters/elasticsearch-starter-Design.md) |
| 11 | [Async](./starters/async-starter-Design.md) |
| 12 | [XXL-JOB](./starters/xxl-job-starter-Design.md) |
| 13 | [Web](./starters/web-starter-Design.md) |
| 14 | [Feign](./starters/feign-starter-Design.md) |
| 15 | [Security](./starters/security-starter-Design.md) |
| 16 | [Drools](./starters/drools-starter-Design.md) |
| 17 | [Audit](./starters/audit-starter-Design.md) |
| 18 | [Field Encryption](./starters/field-encryption-starter-Design.md) |
| 19 | [Object Storage](./starters/object-storage-starter-Design.md) |

## 开发准入

组件开发前必须确认其设计文档中的依赖坐标已由 `microservice-framework-dependencies` 管理、公共 API 已评审、测试环境可获得。完成时必须通过设计文档中的全部测试层级和对应 PRD 验收矩阵。
