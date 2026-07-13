# Microservice Framework

作者：Andy Yang

面向生产级分布式系统的企业级 Java/Spring Boot 微服务应用基座。

[GitHub 首页](./README.md) | [English](./README-en.md) | [公开 PRD](./PRD/README.md) | [贡献指南](./CONTRIBUTING.md) | [安全策略](./SECURITY.md) | [路线图](./ROADMAP.md)

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.6-brightgreen)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)
![Build](https://github.com/andy-library/microservice-framework/actions/workflows/maven-verify.yml/badge.svg)

Microservice Framework 是一套面向企业级微服务体系的 Java/Spring Boot 应用基座。它通过统一 Parent、BOM、Starter 和验收体系，帮助应用团队快速创建可运行、可治理、可观测、可扩展的业务应用，让业务团队尽量专注于业务代码本身。

## 为什么需要这个项目

- 提供 Parent POM、Dependencies BOM、Framework BOM 和 Starter Parent 的统一工程治理。
- 提供 19 个面向生产场景的 Spring Boot Starter。
- 覆盖 Web、Security、Logging、Observability、Redis、Kafka、Database、Elasticsearch、Feign、XXL-JOB、Drools 等企业常见能力。
- 提供独立 Demo 应用，通过 Controller API 验证每个 Starter 面向应用开发者暴露的能力。
- 提供 JMeter 脚本和中英文性能报告，便于社区复现与扩展压测。
- 面向企业中台、高并发 C 端应用和需要统一工程治理的 Spring Boot 微服务团队。

## 开源愿景

Microservice Framework 源于作者 Andy Yang 自 JDK 8 时代开始维护的一套企业级应用脚手架。它曾服务于多个实际项目，并在持续演进中积累了微服务开发、基础设施接入、工程治理和生产运行方面的实践经验。

进入 AI 辅助软件工程时代后，项目在既有实践基础上进行了系统性的二次构建：重新梳理架构边界，统一 Parent、BOM 与 Starter 体系，并补充自动化测试、Demo 验收、真实中间件验证和开源工程规范。AI 参与需求整理、技术设计、代码实现、测试、审查和文档维护，维护者负责目标定义、架构约束、质量标准和最终验收。

本项目开源有三个主要目标：

1. 为社区提供一套可使用、可验证、可扩展的 Java 微服务应用基座，减少企业应用在通用技术能力上的重复建设。
2. 通过真实项目反馈、社区贡献和持续验收，不断改进框架的完整性、可靠性、安全性和生产适用性。
3. 探索 AI 驱动的软件工程模式：验证在明确需求、架构规则、测试标准和治理机制约束下，AI 能否持续承担基础框架的设计、开发、测试、审查和维护工作，降低项目对特定开发人员的依赖。

后续项目变更将优先采用 AI 驱动的工程流程。AI 生成的变更仍必须经过自动化测试、Demo 实战验证、代码审查和维护者验收，不会因使用 AI 而降低质量、安全或兼容性标准。

我们欢迎开发者运行 Demo、验证 Starter、提交问题、补充测试、审查 AI 生成的代码，并提出更适合生产环境的实现方案。社区反馈既用于完善框架，也将成为评估 AI 软件工程能力边界的重要依据。

> 人定义目标、边界和验收标准，AI 执行主要工程工作，自动化证据与社区审查共同验证结果。

## 快速开始

```bash
git clone https://github.com/andy-library/microservice-framework.git
cd microservice-framework

cd SourceCode/microservice-framework-parent
mvn clean install

cd ../microservice-framework-common-starter
mvn clean verify
```

Demo 验证：

```bash
cd "Microservice Demo/SourceCode/microservice-framework-demo"
mvn clean verify
```

## 项目结构

```text
Microservice/
├── README.md
├── README-zh.md
├── README-en.md
├── LICENSE
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── SECURITY.md
├── CHANGELOG.md
├── ROADMAP.md
├── PRD/
│   ├── Requirements/        # 产品需求与各 starter PRD
│   └── Design/              # 技术设计文档与技术栈矩阵
├── SourceCode/
│   ├── microservice-framework-parent/
│   ├── microservice-framework-common-starter/
│   ├── microservice-framework-json-starter/
│   ├── microservice-framework-web-starter/
│   ├── microservice-framework-logging-starter/
│   ├── microservice-framework-observability-starter/
│   ├── microservice-framework-async-starter/
│   ├── microservice-framework-security-starter/
│   ├── microservice-framework-redis-starter/
│   ├── microservice-framework-database-starter/
│   ├── microservice-framework-kafka-starter/
│   ├── microservice-framework-elasticsearch-starter/
│   ├── microservice-framework-feign-starter/
│   ├── microservice-framework-xxl-job-starter/
│   ├── microservice-framework-audit-starter/
│   ├── microservice-framework-field-encryption-starter/
│   ├── microservice-framework-object-storage-starter/
│   ├── microservice-framework-drools-starter/
│   ├── microservice-framework-nacos-starter/
│   └── microservice-framework-apollo-starter/
└── Microservice Demo/
    ├── SourceCode/          # 独立 Demo 源码
    └── Reports/             # JMeter 脚本与最新压测报告
```

Demo 工程已独立迁移到 `Microservice Demo`，与 `PRD`、`SourceCode` 平级。

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 | Java 21 |
| 构建 | Maven 3.9+ |
| 应用框架 | Spring Boot 3.3.13 |
| 微服务 | Spring Cloud 2023.0.6、Spring Cloud OpenFeign |
| 配置中心 | Nacos、Apollo、Kubernetes ConfigMap |
| Web | Spring MVC、Jakarta Validation、Springdoc OpenAPI |
| 安全 | Spring Security、OAuth2 Resource Server、JWT、Keycloak 集成模型 |
| 日志 | SLF4J、Logback、Logstash Logback Encoder |
| 可观测 | Actuator、Micrometer、Micrometer Tracing、OpenTelemetry、Prometheus、OTLP |
| 数据库 | Spring JDBC/Transaction、HikariCP、ShardingSphere-JDBC、Flyway |
| 缓存 | Redis、Spring Data Redis、Lettuce |
| 消息 | Kafka、Spring Kafka |
| 搜索 | Elasticsearch Java API Client |
| 任务 | XXL-JOB |
| 规则引擎 | Drools/KIE |
| 对象存储 | S3 兼容对象存储抽象 |
| 测试 | JUnit 5、AssertJ、Mockito、REST Assured、Spring Boot Test、JMeter |

详细技术栈治理见：

- `PRD/Design/Technology-Stack-Matrix.md`
- `PRD/Design/Engineering-Baseline.md`

## 现有能力

| Starter | 能力摘要 |
| --- | --- |
| `common-starter` | 时区、日期时间、分布式 ID、错误码、分页、轻量上下文和通用工具 |
| `json-starter` | 统一 JSON 抽象，默认 Jackson，可扩展 Fastjson2 |
| `web-starter` | 统一响应、RequestId、参数校验、异常处理、OpenAPI |
| `logging-starter` | 结构化日志、脱敏、异步日志、日志风暴保护、动态日志级别 |
| `observability-starter` | 指标、链路追踪、健康检查、Prometheus、OpenTelemetry/OTLP |
| `async-starter` | 受治理线程池、上下文传播、拒绝策略、优雅停机 |
| `security-starter` | JWT 解析、面向 Keycloak 的资源服务集成、内部可信访问模型 |
| `redis-starter` | 缓存、分布式锁、限流、计数器和 Redis 通用工具 |
| `database-starter` | 数据源治理、事务、读写分离、ShardingSphere-JDBC、outbox |
| `kafka-starter` | 生产者、消费者构建器、手动/自动提交模型、幂等、重试、DLQ |
| `elasticsearch-starter` | 索引、查询、分页、批量操作、别名和运行治理 |
| `feign-starter` | 服务间调用治理、上下文传播、超时、重试、隔离 |
| `xxl-job-starter` | 任务执行、幂等、超时、日志和重试扩展 |
| `audit-starter` | 后台管理动作审计、防篡改审计条目模型 |
| `field-encryption-starter` | 字段级加密抽象和 AES-GCM 默认能力 |
| `object-storage-starter` | S3 兼容对象存储、上传、下载、删除、预签名 URL |
| `drools-starter` | 规则加载、规则执行、版本化规则评估 |
| `nacos-starter` | Nacos + Kubernetes 配置能力 |
| `apollo-starter` | Apollo + Kubernetes 配置能力 |

## Parent 与依赖治理

`microservice-framework-parent` 包含框架级 Maven 治理模型：

- `microservice-framework-parent`：框架内部组件父工程
- `microservice-framework-dependencies`：第三方依赖 BOM
- `microservice-framework-bom`：框架 starter BOM
- `microservice-framework-starter-parent`：业务应用父工程

关键原则：

- Starter 内禁止声明私有依赖版本。
- 第三方版本统一通过 BOM 管理。
- 业务应用继承 starter parent，并按需组合 starter。
- Nacos 与 Apollo 为互斥配置中心选项。

## 构建方式

推荐先安装 Parent 和 BOM，再构建具体 Starter：

示例：

```bash
cd SourceCode/microservice-framework-parent
mvn clean install

cd ../microservice-framework-common-starter
mvn clean install
```

Demo 验收请进入 `Microservice Demo/SourceCode/microservice-framework-demo`。

```bash
cd "Microservice Demo/SourceCode/microservice-framework-demo"
mvn clean verify
```

## 文档

| 目录 | 用途 |
| --- | --- |
| `PRD/Requirements` | Framework PRD、组件目录、Parent PRD、各 starter PRD |
| `PRD/Design` | 技术设计、技术栈、工程基线、集成测试策略 |
| `SourceCode` | Parent 与各 starter 源码 |
| `Microservice Demo/SourceCode` | 独立 Demo 源码 |
| `Microservice Demo/Reports` | JMeter 脚本与最新压测报告 |

## 开源说明

- 本项目为通用微服务框架，不包含公司专属命名。
- 代码作者统一为 `Andy Yang`。
- 构建产物、日志、JMeter 原始结果、本地 IDE 文件已通过根目录 `.gitignore` 忽略。
- 开源协议见仓库根目录 `LICENSE`。
- 贡献指南见 `CONTRIBUTING.md`。
- 安全漏洞披露见 `SECURITY.md`。
- 项目路线图见 `ROADMAP.md`。
- 发布流程见 `RELEASE_GUIDE.md`。
- 仓库 PR 与分支保护建议见 `.github/REPOSITORY_SECURITY_SETTINGS.md`。

## 社区共建

Microservice Framework 的开源目标不是一次性发布，而是通过长期维护持续强化企业级微服务架构能力。欢迎社区开发者、架构师、测试工程师、SRE、安全工程师和业务团队共同参与。

你可以通过以下方式参与：

- 提交 Issue 反馈缺陷、使用问题、能力缺口和设计建议。
- 提交 Pull Request 改进代码、文档、测试、Demo 或 JMeter 脚本。
- 补充真实业务场景下的 starter 验收案例和性能测试结果。
- 参与技术讨论，帮助完善高并发、高可用、安全、可观测和工程治理能力。
- 分享生产落地经验，帮助框架持续进化。

如果该项目对你或你的团队有帮助，也欢迎通过捐赠、赞助或资源支持参与共建。捐赠不是使用门槛，也不会改变项目开放协作原则；它只用于支持项目长期维护、测试环境、文档建设和社区发展。
