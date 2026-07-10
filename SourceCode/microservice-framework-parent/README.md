# Microservice Framework Parent

Author: Andy Yang

Microservice Framework Parent 是企业级微服务框架的构建与版本治理根项目。它提供统一的 Maven 构建规则、第三方依赖 BOM、内部 starter BOM，以及业务应用唯一父 POM。

## 模块结构

| 模块 | 职责 |
| --- | --- |
| `microservice-framework-parent` | 根父 POM，管理构建插件、Enforcer 规则、版本属性、release profile 和 Invoker 契约测试 |
| `microservice-framework-dependencies` | 第三方依赖版本管理 |
| `microservice-framework-bom` | 19 个官方 starter 的版本火车 |
| `microservice-framework-starter-parent` | 业务应用统一父 POM |

## 技术栈

| 技术 | 版本 |
| --- | --- |
| Java | 21 |
| Maven | 3.9.0+ |
| Spring Boot | 3.3.13 |
| Spring Cloud | 2023.0.6 |
| OpenTelemetry | 1.35.0 |
| Micrometer Tracing | 1.4.1 |
| Lombok | 1.18.42 |

## 官方 Starter

当前 BOM 管理 19 个官方 starter：

| 序号 | Starter |
| --- | --- |
| 1 | `microservice-framework-common-starter` |
| 2 | `microservice-framework-json-starter` |
| 3 | `microservice-framework-logging-starter` |
| 4 | `microservice-framework-nacos-starter` |
| 5 | `microservice-framework-apollo-starter` |
| 6 | `microservice-framework-observability-starter` |
| 7 | `microservice-framework-database-starter` |
| 8 | `microservice-framework-redis-starter` |
| 9 | `microservice-framework-kafka-starter` |
| 10 | `microservice-framework-elasticsearch-starter` |
| 11 | `microservice-framework-async-starter` |
| 12 | `microservice-framework-xxl-job-starter` |
| 13 | `microservice-framework-web-starter` |
| 14 | `microservice-framework-feign-starter` |
| 15 | `microservice-framework-security-starter` |
| 16 | `microservice-framework-drools-starter` |
| 17 | `microservice-framework-audit-starter` |
| 18 | `microservice-framework-field-encryption-starter` |
| 19 | `microservice-framework-object-storage-starter` |

## 业务应用接入

业务应用继承 `microservice-framework-starter-parent`：

```xml
<parent>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-starter-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath/>
</parent>
```

引入 starter 时不声明版本号：

```xml
<dependency>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-web-starter</artifactId>
</dependency>
```

## 架构约束

- 根 parent 只负责构建与治理，不声明运行时依赖或 dependencyManagement。
- `microservice-framework-dependencies` 只管理第三方依赖。
- `microservice-framework-bom` 只管理内部 starter。
- 业务应用继承 `microservice-framework-starter-parent`。
- 框架 starter 继承根 `microservice-framework-parent`，不得继承业务 parent。
- 业务应用不得覆盖 `spring-boot.version` 和 `spring-cloud.version`。

## 验证命令

```bash
mvn install -DskipTests
bash scripts/verify-parent-contracts.sh
```

`mvn install -DskipTests` 会执行 Maven Invoker 消费方契约。`verify-parent-contracts.sh` 会补充静态职责边界检查和业务/starter 消费方检查。

## 发布

正式发布时使用 release profile：

```bash
mvn clean deploy -Prelease -Drevision=<release-version>
```

release profile 会附加 sources 和 javadocs。实际仓库地址由企业 Maven 仓库配置负责。
