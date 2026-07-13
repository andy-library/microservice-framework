# Microservice Framework

作者：Andy Yang

面向生产级分布式系统的企业级 Java/Spring Boot 微服务应用基座。

[中文](./README.md) | [English](./README-en.md) | [公开 PRD](./PRD/README.md) | [贡献指南](./CONTRIBUTING.md) | [安全策略](./SECURITY.md) | [路线图](./ROADMAP.md)

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.6-brightgreen)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)
![Build](https://github.com/andy-library/microservice-framework/actions/workflows/maven-verify.yml/badge.svg)

Microservice Framework 通过统一 Parent、BOM、Starter、Demo 和验收体系，为企业级微服务应用提供标准化基座。应用团队可以按需组合能力并直接开发业务代码，框架负责工程治理、基础设施接入、安全边界、可观测性和生产运行约束。

## 项目定位

- 为企业中台和高并发 C 端应用提供统一的微服务开发基座。
- 通过 Parent POM 与 BOM 集中治理 Java、Spring Boot、Spring Cloud、插件和第三方依赖版本。
- 提供 19 个生产导向的 Spring Boot Starter，支持默认组合和按需扩展。
- 使用独立 Demo Controller API 验证所有面向应用开发者开放的能力。
- 同时覆盖无外部依赖的嵌入式验收和真实本地中间件验收。
- 让业务团队专注于业务实现，避免重复建设通用基础能力。

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

## 技术基线

| 分类 | 技术 |
| --- | --- |
| 语言与构建 | Java 21、Maven 3.9+ |
| 应用框架 | Spring Boot 3.3.13、Spring Cloud 2023.0.6 |
| Web 与调用 | Spring MVC、Jakarta Validation、OpenFeign |
| 安全 | Spring Security、OAuth2 Resource Server、JWT、Keycloak 集成模型 |
| 配置 | Nacos、Apollo、Kubernetes ConfigMap |
| 数据 | MySQL、HikariCP、ShardingSphere-JDBC、Redis、Elasticsearch |
| 消息与任务 | Kafka、XXL-JOB、受治理异步线程池 |
| 可观测 | Actuator、Micrometer、OpenTelemetry、Prometheus、OTLP |
| 规则与存储 | Drools/KIE、S3 兼容对象存储 |
| 测试 | JUnit 5、AssertJ、Mockito、REST Assured、JMeter |

详细版本和兼容性约束见 [技术栈矩阵](./PRD/Design/Technology-Stack-Matrix.md) 与 [工程基线](./PRD/Design/Engineering-Baseline.md)。

## Starter 能力

| Starter | 核心能力 |
| --- | --- |
| `common-starter` | 日期时间、时区、分布式 ID、错误码、分页、轻量上下文和通用工具 |
| `json-starter` | 统一 JSON 抽象，默认 Jackson，可选 Fastjson2 |
| `web-starter` | 统一响应、RequestId、参数校验、异常处理和 Web 治理 |
| `logging-starter` | 结构化日志、脱敏、异步输出、风暴保护和日志级别治理 |
| `observability-starter` | 指标、链路追踪、健康检查、Prometheus 和 OTLP 接入 |
| `async-starter` | 受治理线程池、上下文传播、拒绝策略和优雅停机 |
| `security-starter` | JWT 资源服务、接口权限、内部可信服务身份和安全异常响应 |
| `redis-starter` | 缓存、批量失效、分布式锁、限流、计数器和通用 Redis 操作 |
| `database-starter` | 数据源、事务、读写分离、ShardingSphere-JDBC 和 Outbox |
| `kafka-starter` | 可靠生产、消费模型、手动/自动提交、DLQ、幂等和 Outbox 发布 |
| `elasticsearch-starter` | 索引、查询、分页、批量操作、别名和运行治理 |
| `feign-starter` | 服务调用、上下文透传、服务身份、连接池、超时、重试和指标 |
| `xxl-job-starter` | 任务执行、幂等、超时和生命周期治理 |
| `audit-starter` | 后台管理动作审计和防篡改校验 |
| `field-encryption-starter` | 字段级加密抽象和 AES-GCM 默认实现 |
| `object-storage-starter` | S3/MinIO、上传、下载、删除和预签名 URL |
| `drools-starter` | 规则加载、执行、校验和版本信息 |
| `nacos-starter` | Nacos 与 Kubernetes 配置接入和配置治理 |
| `apollo-starter` | Apollo 与 Kubernetes 配置接入和配置治理 |

## 快速开始

环境要求：JDK 21、Maven 3.9+。

```bash
git clone https://github.com/andy-library/microservice-framework.git
cd microservice-framework

cd SourceCode/microservice-framework-parent
mvn clean install
```

构建某个 Starter：

```bash
cd ../microservice-framework-common-starter
mvn clean verify
```

运行 Demo 的嵌入式全量验收：

```bash
cd "../../Microservice Demo/SourceCode/microservice-framework-demo"
mvn clean verify
```

运行需要本地 MySQL、Redis、Kafka、Elasticsearch、Nacos 和 Apollo 的真实验收：

```bash
mvn verify -Preal-middleware-acceptance \
  -Ddemo.real.middleware.acceptance=true
```

## 验收基线

当前 `main` 已验证：

- Demo 嵌入式套件：116 项测试，0 失败；28 项真实环境测试按设计跳过。
- Demo 真实中间件套件：28 项测试，0 失败。
- Parent 消费者契约：9 项场景，全部通过。
- GitHub Actions：Parent、19 个 Starter、消费者契约和 Demo 干净构建全部通过。

真实验收覆盖 MySQL、Redis、Kafka、Elasticsearch、Nacos、Apollo，以及 Security、Feign 和各 Starter 的应用侧 Controller API。

## 项目结构

```text
microservice-framework/
├── README.md                 # GitHub 默认中文入口
├── README-zh.md              # 中文文档
├── README-en.md              # English documentation
├── LICENSE
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── SECURITY.md
├── CHANGELOG.md
├── ROADMAP.md
├── PRD/
│   ├── Requirements/         # Framework、Parent 与各 Starter PRD
│   └── Design/               # 技术设计、工程基线和测试设计
├── SourceCode/               # Parent、BOM 与 19 个 Starter
└── Microservice Demo/
    ├── SourceCode/           # 独立可运行 Demo
    └── Reports/              # JMeter 脚本与正式测试报告
```

## 工程治理

- Starter 不独立声明受保护的依赖版本，统一由 Dependencies BOM 管理。
- 业务应用继承 Starter Parent，并按需引入 Starter。
- Nacos 与 Apollo 是互斥的配置中心选项，不允许同时启用。
- 外部公网访问由部署环境中的 API Gateway 管理，不属于应用 Starter。
- 内部服务调用使用 Feign，并支持用户上下文与服务身份透传。
- 真实数据库表结构升级应由 Flyway/Liquibase 等迁移工具管理，Starter 自动建表仅用于 Demo 或开发环境。

## 文档与社区

- [公开 PRD 总入口](./PRD/README.md)
- [需求文档](./PRD/Requirements/README.md)
- [技术设计](./PRD/Design/README.md)
- [贡献指南](./CONTRIBUTING.md)
- [行为准则](./CODE_OF_CONDUCT.md)
- [安全策略](./SECURITY.md)
- [发布指南](./RELEASE_GUIDE.md)
- [项目路线图](./ROADMAP.md)

欢迎架构师、开发者、测试工程师、SRE 和安全工程师通过 Issue 与 Pull Request 共同维护项目。若项目对你或团队有帮助，也欢迎通过赞助、测试资源或基础设施支持长期维护；这不是使用门槛，也不会改变开放协作原则。

## License

本项目采用 [Apache License 2.0](./LICENSE)。
