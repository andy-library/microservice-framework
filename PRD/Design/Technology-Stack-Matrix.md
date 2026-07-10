# 组件技术栈矩阵

Author: Andy Yang

本矩阵结合真实 Parent POM 与冻结 PRD，定义每个组件开发时允许使用的主要技术。版本只能来自 Parent/Dependencies BOM。

## 1. Parent 已锁定版本

| 技术 | 版本 |
| --- | --- |
| Java | 21 |
| Maven | 3.9.0+ |
| Spring Boot | 3.3.13 |
| Spring Cloud | 2023.0.6 |
| OpenTelemetry BOM | 1.35.0 |
| Micrometer Tracing BOM | 1.4.1 |
| Fastjson2 | 2.0.54 |
| Logstash Logback Encoder | 7.4 |
| Lombok | 1.18.42 |
| REST Assured | 5.3.2 |

## 2. 组件技术栈

| 组件 | 主要技术 | 版本来源 |
| --- | --- | --- |
| Parent | Maven Enforcer、Invoker、Surefire、Failsafe、Flatten、Boot Maven Plugin | 根 Parent |
| Common | Java Time、Jakarta Validation、Spring Boot Autoconfigure | Boot BOM/JDK |
| JSON | Jackson、Fastjson2 | Boot BOM / Dependencies BOM |
| Logging | SLF4J、Logback、Logstash Encoder、Janino | Boot/Dependencies BOM |
| Nacos | Spring Cloud Alibaba Nacos Config、Boot Config Data、K8s 原生配置 | 新增 Dependencies BOM |
| Apollo | Apollo Java Client、Boot Environment/Config Data、K8s 原生配置 | 新增 Dependencies BOM |
| Observability | Actuator、Micrometer、Micrometer Tracing、OpenTelemetry、OTLP、Prometheus | Boot/Dependencies BOM |
| Database | Spring JDBC/Transaction、HikariCP、ShardingSphere-JDBC、Flyway | Boot BOM / 新增 Dependencies BOM |
| Redis | Spring Data Redis、Lettuce、Commons Pool2 | Boot BOM |
| Kafka | Spring Kafka、Kafka Client | Boot BOM |
| Elasticsearch | Elasticsearch Java API Client | 新增 Dependencies BOM |
| Async | Java Executor、Spring Task Execution、Micrometer optional | JDK/Boot BOM |
| XXL-JOB | XXL-JOB Executor | 新增 Dependencies BOM |
| Web | Spring MVC、Jakarta Validation、Springdoc OpenAPI | Boot BOM / 新增 Dependencies BOM |
| Feign | Spring Cloud OpenFeign、受治理 HTTP Client、Resilience4j | Cloud/Boot BOM |
| Security | Spring Security、OAuth 2 Resource Server、Nimbus JOSE JWT | Boot/Security BOM |
| Drools | Drools/KIE | 新增 Dependencies BOM |
| Audit | Spring AOP/Transaction、Database Outbox 或 Kafka | Boot BOM / Framework BOM |
| Field Encryption | Java Cryptography Architecture、AES-GCM、KMS SPI | JDK / Provider 由 BOM 管理 |
| Object Storage | AWS SDK v2 S3 API | 新增 Dependencies BOM |

## 3. 测试技术栈

| 测试目标 | 技术 |
| --- | --- |
| 单元与断言 | JUnit 5、AssertJ、Mockito |
| 自动配置 | Spring Boot `ApplicationContextRunner` |
| Web/Security | MockMvc、REST Assured |
| 真实中间件 | Testcontainers 对应模块 |
| HTTP 下游 | WireMock 或 MockWebServer，统一选择后由 BOM 管理 |
| 异步等待 | Awaitility |
| 依赖和消费方契约 | Maven Enforcer、Maven Invoker |
| 架构边界 | Enforcer；需要包规则时使用 ArchUnit |
| 故障注入 | Toxiproxy/Testcontainers 或受控替身 |
| 微基准 | JMH |

## 4. 纳管准入

“新增 Dependencies BOM”的技术在开发前必须完成兼容性验证，并将批准版本、依赖坐标和必要排除项加入 `microservice-framework-dependencies`。Starter 不得先行声明私有版本。
