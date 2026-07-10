# Microservice Framework Parent 项目状态

Author: Andy Yang

**最后更新时间**: 2026-06-19  
**当前状态**: Parent 生产级打磨与验收  
**当前组件**: `microservice-framework-parent`

## 1. 项目定位

`microservice-framework-parent` 是框架基座的构建与版本治理根项目，不承载业务运行时能力。它负责统一 Java/Maven 构建规范、第三方依赖版本、内部 starter 版本火车，以及业务应用统一父 POM。

## 2. 模块结构

| 模块 | 职责 |
| --- | --- |
| `microservice-framework-parent` | 根父 POM，管理构建插件、Enforcer 规则、版本属性、发布 Profile 和 Invoker 契约测试 |
| `microservice-framework-dependencies` | 第三方依赖 BOM，导入 Spring Boot、Spring Cloud 及框架批准的第三方组件版本 |
| `microservice-framework-bom` | 内部 starter BOM，管理 19 个官方 starter 的版本 |
| `microservice-framework-starter-parent` | 业务应用唯一父 POM，聚合外部依赖 BOM 和内部 starter BOM，提供业务应用构建默认能力 |

## 3. 当前技术栈

| 项 | 当前值 |
| --- | --- |
| Java | 21 |
| Maven | 3.9.0+ |
| Spring Boot | 3.3.13 |
| Spring Cloud | 2023.0.6 |
| OpenTelemetry | 1.35.0 |
| Micrometer Tracing | 1.4.1 |
| Lombok | 1.18.42 |

## 4. 官方 Starter 清单

当前版本火车包含 19 个官方 starter：

1. `microservice-framework-common-starter`
2. `microservice-framework-json-starter`
3. `microservice-framework-logging-starter`
4. `microservice-framework-nacos-starter`
5. `microservice-framework-apollo-starter`
6. `microservice-framework-observability-starter`
7. `microservice-framework-database-starter`
8. `microservice-framework-redis-starter`
9. `microservice-framework-kafka-starter`
10. `microservice-framework-elasticsearch-starter`
11. `microservice-framework-async-starter`
12. `microservice-framework-xxl-job-starter`
13. `microservice-framework-web-starter`
14. `microservice-framework-feign-starter`
15. `microservice-framework-security-starter`
16. `microservice-framework-drools-starter`
17. `microservice-framework-audit-starter`
18. `microservice-framework-field-encryption-starter`
19. `microservice-framework-object-storage-starter`

## 5. Parent 生产准入

Parent 组件进入生产可用状态前必须满足：

- `mvn install -DskipTests` 通过。
- Maven Invoker 消费方契约全部通过，负面场景必须按预期失败。
- `scripts/verify-parent-contracts.sh` 通过。
- 根 parent 不声明运行时 `<dependencies>` 或 `<dependencyManagement>`。
- `microservice-framework-bom` 只管理 19 个官方 starter，不管理第三方依赖。
- `microservice-framework-dependencies` 只管理第三方依赖，不管理内部 starter。
- `microservice-framework-starter-parent` 可被业务应用继承，并可不写版本号解析第三方依赖与 19 个官方 starter。
- 业务应用继承 `starter-parent` 后可通过 Spring Boot Maven Plugin 打包可执行应用；框架 starter 不应被打成 fat jar。

## 6. 当前打磨重点

本轮只处理 parent 父项目。parent 完成并验收后，再进入下一个 starter 组件。
