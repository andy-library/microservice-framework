# Microservice Framework 正式组件目录

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 目录版本 | v1.0 |
| 文档状态 | 当前设计基线 |
| 组件原则 | 能力边界清晰、基础能力强制、扩展能力按需、相近能力适度聚合 |

## 1. Parent 制品

| 制品 | 职责 |
| --- | --- |
| `microservice-framework-parent` | Framework 内部组件构建治理 |
| `microservice-framework-dependencies` | 第三方依赖版本治理 |
| `microservice-framework-bom` | 全部正式 Starter 版本管理 |
| `microservice-framework-starter-parent` | 业务应用构建治理 |

## 2. 正式 Starter

| 顺序 | Starter | 引入策略 | 核心职责 |
| ---: | --- | --- | --- |
| 1 | `common-starter` | 所有应用强制 | 日期、时区、ID、错误码、分页、金额、轻量上下文和通用工具 |
| 2 | `json-starter` | 所有应用强制 | 统一 JSON API；默认 Jackson，可显式选择 Fastjson2 |
| 3 | `logging-starter` | 所有应用强制 | 结构化日志、脱敏、异步输出、风暴保护和动态级别 |
| 4 | `nacos-starter` | 与 Apollo 二选一 | Nacos 与 Kubernetes 配置能力 |
| 5 | `apollo-starter` | 与 Nacos 二选一 | Apollo 与 Kubernetes 配置能力 |
| 6 | `observability-starter` | 生产默认推荐、按需 | 指标、追踪、健康检查、Prometheus、OTLP 和告警接入 |
| 7 | `database-starter` | 按需 | ShardingSphere-JDBC、读写分离、事务、幂等和 Outbox |
| 8 | `redis-starter` | 按需 | 缓存、锁、限流、计数器和排行榜 |
| 9 | `kafka-starter` | 按需 | Kafka 生产、消费、提交、幂等、重试和死信 |
| 10 | `elasticsearch-starter` | 按需 | Elasticsearch 索引、查询、批量处理和运行治理 |
| 11 | `async-starter` | 按需 | 受治理线程池、上下文传播、拒绝策略和优雅停机 |
| 12 | `xxl-job-starter` | 按需 | XXL-JOB 执行、幂等、超时、日志、告警和人工重试 |
| 13 | `web-starter` | Web 应用默认 | HTTP 入口、统一响应、异常、校验、RequestId 和 OpenAPI |
| 14 | `feign-starter` | 微服务调用按需 | 服务身份、上下文传播、连接、超时、重试、熔断和隔离 |
| 15 | `security-starter` | Web 默认、其他按需 | Keycloak JWT、服务身份和接口权限 |
| 16 | `drools-starter` | 按需 | Drools 规则加载、执行、版本和运行治理 |
| 17 | `audit-starter` | B 端强制、其他按需 | 操作审计、可靠投递和防篡改 |
| 18 | `field-encryption-starter` | 按需 | 字段加密、密钥隔离、轮换和密文治理 |
| 19 | `object-storage-starter` | 按需 | S3 兼容对象存储、预签名访问和文件治理 |

## 3. 内部复用制品

内部复用制品用于减少重复实现，但不作为业务应用直接选择的 Starter：

| 建议制品 | 用途 |
| --- | --- |
| `config-core` | Nacos 与 Apollo Starter 共用的配置校验、来源和敏感配置治理 |
| `test-support` | Starter 自动配置、契约和集成测试公共能力 |

内部制品默认不面向业务应用直接暴露，由框架构建、测试和组件复用场景使用。

## 4. 关键依赖规则

- Parent 不引入任何运行时 Starter。
- Common 不依赖其他 Framework Starter。
- JSON 依赖 Common，不依赖 Web。
- Logging 依赖 Common 和 JSON，不依赖 Observability 或特定配置中心。
- Observability 可以依赖 Logging。
- Nacos 与 Apollo Starter 禁止同时引入。
- Async 是通用能力，独立于 XXL-JOB。
- XXL-JOB 可以复用 Async 的上下文和治理契约。
- Kafka 使用自身消费线程模型，不复用普通业务线程池。
- Web 默认组合 Security，但两者保持独立 Starter。
- Feign 可选接入 Observability，未引入时仍必须保留 Logging 和超时治理。
- Database 默认读写分离，分库分表、幂等和 Outbox 均显式启用。
- Elasticsearch 第一版只支持 Elasticsearch，不抽象 OpenSearch。

## 5. 不纳入独立 Starter 的能力

| 能力 | 处理方式 |
| --- | --- |
| Resilience4j | 聚合到 Feign 等调用方 Starter |
| ShardingSphere-JDBC | 聚合到 Database |
| Outbox 与数据库幂等 | 聚合到 Database |
| Date 与 ID | 聚合到 Common |
| Gateway | 外部组件，不属于应用 Framework |
| Service Discovery | 当前不独立建立 |
| OpenSearch | 第一版不支持 |
