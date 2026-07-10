# Microservice Framework 当前实现基线

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 文档版本 | v1.0 |
| 文档状态 | 当前实现基线 |
| 代码位置 | `SourceCode`、`Microservice Demo/SourceCode` |
| 实现范围 | Parent、BOM、19 个 Starter、Demo 应用 |

## 1. 正式源码模块

| # | 项目 | 测试数 | 状态 |
| --- | --- | --- | --- |
| 1 | `microservice-framework-parent` | 9 契约 | BUILD SUCCESS |
| 2 | `microservice-framework-common-starter` | 193 | BUILD SUCCESS |
| 3 | `microservice-framework-json-starter` | 60 | BUILD SUCCESS |
| 4 | `microservice-framework-logging-starter` | 98 | BUILD SUCCESS |
| 5 | `microservice-framework-observability-starter` | 79 | BUILD SUCCESS |
| 6 | `microservice-framework-nacos-starter` | 102 | BUILD SUCCESS |
| 7 | `microservice-framework-apollo-starter` | 120 | BUILD SUCCESS |
| 8 | `microservice-framework-database-starter` | 35 | BUILD SUCCESS |
| 9 | `microservice-framework-redis-starter` | 74 | BUILD SUCCESS |
| 10 | `microservice-framework-kafka-starter` | 25 | BUILD SUCCESS |
| 11 | `microservice-framework-elasticsearch-starter` | 61 | BUILD SUCCESS |
| 12 | `microservice-framework-async-starter` | 33 | BUILD SUCCESS |
| 13 | `microservice-framework-xxl-job-starter` | 39 | BUILD SUCCESS |
| 14 | `microservice-framework-web-starter` | 84 | BUILD SUCCESS |
| 15 | `microservice-framework-feign-starter` | 60 | BUILD SUCCESS |
| 16 | `microservice-framework-security-starter` | 58 | BUILD SUCCESS |
| 17 | `microservice-framework-drools-starter` | 28 | BUILD SUCCESS |
| 18 | `microservice-framework-audit-starter` | 32 | BUILD SUCCESS |
| 19 | `microservice-framework-field-encryption-starter` | 33 | BUILD SUCCESS |
| 20 | `microservice-framework-object-storage-starter` | 78 | BUILD SUCCESS |
| 21 | `microservice-framework-demo` | 25 | BUILD SUCCESS |

## 2. 验收基线

- 全部 20 个项目 + Demo 的 `mvn clean verify` 全量回归通过
- Parent 9 个契约 Fixture 全部通过（含 protected-version-override-fails）
- Parent 三组 Enforcer 规则全部通过（enforce-versions、enforce-governance、enforce-protected-versions）
- 总测试数约 1097 + 9 契约 + 25 Demo，0 失败

## 3. Starter 实现范围

每个 Starter 实现了：

- **公共 API 层**：核心接口和值对象
- **Properties**：`@ConfigurationProperties(prefix = "framework.<name>")`
- **自动配置**：`@AutoConfiguration` + 条件注解
- **单元测试**：API 契约、属性绑定、自动配置激活/禁用

重型第三方能力通过 BOM、条件装配和 starter 独立配置进行治理，避免基础应用被无关运行时依赖污染。

## 4. 构建治理基线

- Parent 不引入运行时 Starter。
- Spring Boot、Spring Cloud、Maven 插件和第三方依赖由 Parent 与 BOM 统一治理。
- 受保护版本不得被业务应用覆盖。
- Starter 必须支持独立构建、独立测试和自动配置条件验证。
- Demo 应用用于验证 starter 面向应用开发者暴露的实际能力。

## 5. 能力边界基线

- Logging 与 Observability 独立，Logging 为强制基础能力。
- Nacos 与 Apollo 为二选一配置中心 starter。
- Database 默认提供读写分离能力，分库分表、幂等和 Outbox 显式启用。
- Redis、Kafka、Elasticsearch、XXL-JOB、Drools、对象存储按需引入。
- Web 与 Security 保持独立，可在 Web 应用中组合使用。
- Audit 面向 B 端管理类应用强制，其他应用按需。
