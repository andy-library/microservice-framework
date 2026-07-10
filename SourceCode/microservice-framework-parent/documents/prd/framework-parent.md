# Microservice Framework Parent PRD

Author: Andy Yang

## 1. 目标

`microservice-framework-parent` 提供企业级微服务框架的构建、版本和消费方契约治理。它的目标是让业务团队通过一个标准父 POM 和一组按需 starter 快速创建可运行、可治理、可升级的应用基座。

本项目不实现业务运行时能力。运行时能力由独立 starter 提供。

## 2. 模块定义

| 模块 | 类型 | 目标 |
| --- | --- | --- |
| `microservice-framework-parent` | root parent | 统一 Maven 插件、Java 版本、Enforcer 规则、release profile、Invoker 契约测试 |
| `microservice-framework-dependencies` | dependency BOM | 统一第三方依赖版本 |
| `microservice-framework-bom` | framework BOM | 管理 19 个官方 starter 的版本火车 |
| `microservice-framework-starter-parent` | business parent | 业务应用唯一继承入口，聚合第三方 BOM 与框架 BOM |

## 3. 技术栈

| 技术 | 版本 |
| --- | --- |
| Java | 21 |
| Maven | 3.9.0+ |
| Spring Boot | 3.3.13 |
| Spring Cloud | 2023.0.6 |
| OpenTelemetry | 1.35.0 |
| Micrometer Tracing | 1.4.1 |
| Lombok | 1.18.42 |
| Maven Enforcer Plugin | 3.4.1 |
| Flatten Maven Plugin | 1.6.0 |

## 4. 官方 Starter 清单

`microservice-framework-bom` 必须且只能管理以下 19 个 starter：

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

## 5. 架构约束

### 5.1 Root Parent

- 不得声明运行时 `<dependencies>`。
- 不得声明 `<dependencyManagement>`。
- 必须统一 Maven 插件版本。
- 必须启用 Enforcer 架构守护。
- 必须支持 `${revision}` 与 flatten 发布模式。
- 必须提供 release profile，用于生成 sources 和 javadocs。

### 5.2 Dependencies BOM

- 只管理第三方依赖。
- 不得管理任何 `microservice-framework-*-starter`。
- 必须导入 Spring Boot BOM 和 Spring Cloud BOM。

### 5.3 Framework BOM

- 只管理内部 starter。
- 每个 starter 版本必须使用 `${project.version}`。
- 不得管理第三方依赖。

### 5.4 Starter Parent

- 业务应用必须继承它。
- 必须导入 `microservice-framework-dependencies` 和 `microservice-framework-bom`。
- 必须提供业务应用打包所需的 Spring Boot Maven Plugin 默认能力。
- 框架内部 starter 不得继承它，避免类库被错误打成 fat jar。

## 6. Enforcer 规则

必须强制执行：

- Maven 版本不低于 3.9.0。
- Java 版本不低于 21。
- 依赖收敛。
- Require Upper Bound Dependencies。
- 所有 Maven 插件显式声明版本。
- 禁止重复类冲突。
- 禁止动态版本。
- 禁止危险依赖：`log4j:log4j`、`org.slf4j:slf4j-log4j12`、`com.alibaba:fastjson`。
- 业务应用不得覆盖 `spring-boot.version` 和 `spring-cloud.version`。

## 7. 验收标准

Parent 组件达到生产可用必须满足：

- `mvn install -DskipTests` 构建成功。
- Maven Invoker 消费方契约全部通过。
- 负面契约必须按预期失败，包括危险依赖、版本覆盖、依赖冲突。
- `scripts/verify-parent-contracts.sh` 执行成功。
- 业务应用 fixture 可不写版本号解析第三方依赖和 19 个官方 starter。
- BOM/Dependencies/root parent 职责边界由脚本和 Invoker 测试共同验证。

## 8. 非目标

- 不聚合各 starter 源码。
- 不提供任何业务运行时 Bean。
- 不替代 Kubernetes、Gateway、Nacos、Apollo 等外部平台组件。
- 不在 parent 中定义业务默认配置。
