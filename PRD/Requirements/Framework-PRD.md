# Microservice Framework 产品需求文档

作者：Andy Yang

| 属性 | 内容 |
| --- | --- |
| 文档版本 | v1.0 |
| 文档状态 | 当前设计基线 |
| 产品名称 | Microservice Framework |
| 产品类型 | 企业级 Java 微服务应用基座 |
| 运行环境 | Kubernetes |
| 技术基线 | Java 21、Maven 3.9+、Spring Boot 3.3.13、Spring Cloud 2023.0.6 |

## 1. 产品定位

Microservice Framework 是面向企业级中台与高并发微服务体系的统一应用基座。框架通过 Parent、BOM、Starter、Demo 验收工程和工程规范，为业务应用提供可复用、可治理、可观测、可扩展的技术底座。

框架目标是让应用团队在创建项目时，通过标准 Parent 与按需 Starter 组合获得可直接运行的业务应用基座，并将通用工程能力、治理能力和质量门禁统一收敛到框架层。

## 2. 目标用户

| 用户 | 诉求 |
| --- | --- |
| 应用开发团队 | 快速创建微服务应用，减少重复建设，聚焦业务开发 |
| 架构团队 | 统一技术栈、依赖版本、工程规范、运行治理和质量标准 |
| 运维与 SRE 团队 | 获得统一日志、指标、链路、健康检查、优雅停机和故障诊断能力 |
| 安全团队 | 统一鉴权、服务身份、敏感数据保护、审计和安全默认值 |

## 3. 核心目标

1. 提供统一的 Parent、Dependencies BOM、Framework BOM 和 Starter Parent。
2. 提供可显式组合的 Starter 能力，应用按业务需要选择引入。
3. 强制基础能力由应用显式声明，Parent 不隐式传递运行时 Starter。
4. 统一技术栈、依赖版本、配置前缀、错误码、响应结构和工程边界。
5. 提供高并发 C 端场景所需的缓存、异步、消息、数据库、搜索和安全治理能力。
6. 提供可观测、日志、审计、加密、对象存储、规则引擎和定时任务等企业级扩展能力。
7. 提供独立 Demo 工程与性能脚本，验证 starter 在真实应用中的组合能力。

## 4. 仓库结构

GitHub 主目录为 `Microservice`：

```text
Microservice/
├── README.md
├── README-zh.md
├── README-en.md
├── LICENSE
├── PRD/
│   ├── Requirements/
│   └── Design/
├── SourceCode/
└── Microservice Demo/
    ├── SourceCode/
    └── Reports/
```

| 目录 | 内容 |
| --- | --- |
| `PRD/Requirements` | 框架级 PRD、Parent PRD、组件目录和各 starter PRD |
| `PRD/Design` | 技术设计、技术栈矩阵、工程基线和 starter 设计 |
| `SourceCode` | Parent、BOM、starter 源码 |
| `Microservice Demo/SourceCode` | 独立 Demo 应用源码 |
| `Microservice Demo/Reports` | JMeter 脚本和最新压测报告 |

## 5. 命名规范

| 对象 | 规范 |
| --- | --- |
| Maven GroupId | `com.microservice.framework` |
| Java 根包名 | `com.microservice.framework` |
| 制品前缀 | `microservice-framework-` |
| 配置前缀 | `framework.*` |
| GitHub 主目录 | `Microservice` |

## 6. Framework 制品

| 制品 | 职责 |
| --- | --- |
| `microservice-framework-parent` | 框架内部组件构建治理 |
| `microservice-framework-dependencies` | 第三方依赖版本治理 |
| `microservice-framework-bom` | 框架 starter 版本治理 |
| `microservice-framework-starter-parent` | 业务应用父工程 |
| `microservice-framework-*-starter` | 独立业务能力 starter |
| `microservice-framework-demo` | 独立 Demo 验收应用 |

## 7. 架构分层

| 分层 | 组件 |
| --- | --- |
| 构建治理层 | Parent、Dependencies BOM、Framework BOM、Starter Parent |
| 强制基础层 | Common、JSON、Logging、Nacos 或 Apollo |
| 应用入口层 | Web、Security |
| 服务调用层 | Feign |
| 数据与中间件层 | Database、Redis、Kafka、Elasticsearch、Object Storage |
| 执行与规则层 | Async、XXL-JOB、Drools |
| 治理增强层 | Observability、Audit、Field Encryption |

## 8. 技术栈

| 领域 | 当前技术栈 |
| --- | --- |
| 语言与构建 | Java 21、Maven 3.9+ |
| 应用框架 | Spring Boot 3.3.13、Spring Cloud 2023.0.6 |
| 运行平台 | Kubernetes、Kubernetes DNS |
| 配置中心 | Nacos + Kubernetes，或 Apollo + Kubernetes |
| Web | Spring MVC、Jakarta Validation、Springdoc OpenAPI |
| 安全 | Spring Security、OAuth2 Resource Server、JWT、Keycloak 集成模型 |
| 日志 | SLF4J、Logback、Logstash Logback Encoder |
| 可观测 | Actuator、Micrometer、Micrometer Tracing、OpenTelemetry、Prometheus、OTLP |
| 数据库 | Spring JDBC/Transaction、HikariCP、ShardingSphere-JDBC、Flyway |
| 缓存 | Redis、Spring Data Redis、Lettuce |
| 消息 | Kafka、Spring Kafka |
| 搜索 | Elasticsearch Java API Client |
| 定时任务 | XXL-JOB |
| 规则引擎 | Drools/KIE |
| 对象存储 | S3 兼容对象存储抽象 |

## 9. Starter 能力要求

| Starter | 引入策略 | 核心能力 |
| --- | --- | --- |
| `common-starter` | 强制 | 日期时间、时区、分布式 ID、错误码、分页、轻量上下文、通用工具 |
| `json-starter` | 强制 | 统一 JSON API，默认 Jackson，可选择 Fastjson2 |
| `logging-starter` | 强制 | 结构化日志、脱敏、异步输出、日志风暴保护、动态日志级别 |
| `nacos-starter` | 与 Apollo 二选一 | Nacos 与 Kubernetes 配置能力 |
| `apollo-starter` | 与 Nacos 二选一 | Apollo 与 Kubernetes 配置能力 |
| `observability-starter` | 生产推荐 | 指标、链路追踪、健康检查、Prometheus、OTLP |
| `web-starter` | Web 应用默认 | HTTP 入口、统一响应、异常处理、校验、RequestId、OpenAPI |
| `security-starter` | Web 默认 | JWT 解析、接口权限、服务身份、内部可信访问 |
| `feign-starter` | 微服务调用按需 | 服务身份、上下文传播、连接池、超时、重试、隔离 |
| `database-starter` | 按需 | 数据源治理、事务、读写分离、ShardingSphere-JDBC、幂等、Outbox |
| `redis-starter` | 按需 | 缓存、分布式锁、限流、计数器和 Redis 工具 |
| `kafka-starter` | 按需 | 生产、消费、提交策略、幂等、重试、死信 |
| `elasticsearch-starter` | 按需 | 索引、查询、分页、批量处理、别名和运行治理 |
| `async-starter` | 按需 | 受治理线程池、上下文传播、拒绝策略、优雅停机 |
| `xxl-job-starter` | 按需 | XXL-JOB 执行、幂等、超时、日志、告警和人工重试 |
| `drools-starter` | 按需 | 规则加载、执行、版本和运行治理 |
| `audit-starter` | B 端强制，其他按需 | 操作审计、可靠记录、防篡改 |
| `field-encryption-starter` | 按需 | 字段加密、密钥隔离、轮换和密文治理 |
| `object-storage-starter` | 按需 | S3 兼容对象存储、预签名访问、文件治理 |

## 10. 强制基础组合

所有业务应用必须显式引入：

```text
common-starter
json-starter
logging-starter
nacos-starter 或 apollo-starter
```

生产应用推荐显式引入 `observability-starter`。未引入时，应用必须提供兼容的健康检查、诊断与监控能力。

## 11. 全局约束

1. Parent 不隐式引入运行时 Starter。
2. Starter 必须独立构建、独立测试，并由 Framework BOM 管理版本。
3. Starter 自动配置必须具备启用条件、配置校验、失败诊断和用户覆盖能力。
4. 强制能力不得被普通应用随意关闭。
5. Nacos 与 Apollo 只能启用一个配置中心方案。
6. 每个微服务默认拥有独立数据库。
7. 内部同步调用使用 Feign 与服务身份，禁止逐次远程鉴权影响高并发性能。
8. 所有线程池、连接池、队列、批量、超时、重试必须有容量边界。
9. 敏感数据不得进入日志、Trace、指标、错误响应或不安全配置。
10. 安全与数据正确性能力失败时必须失败关闭。
11. 辅助能力故障时必须告警、记录诊断信息，并优先保障核心业务运行。
12. 所有应用必须支持 Kubernetes 优雅停机和滚动发布。

## 12. 数据与缓存要求

1. Database 默认集成 ShardingSphere-JDBC。
2. Database 默认使用读写分离能力，分库分表默认关闭。
3. Redis 作为统一分布式缓存基础组件。
4. 前端热点读取必须优先命中缓存。
5. 非读操作完成后必须触发缓存更新或失效。
6. 缓存不允许全表缓存，缓存类型由业务场景定义。
7. 极短暂缓存不一致可接受，但必须可观测、可补偿。

## 13. 安全要求

1. 公网入口由 API Gateway 与 Keycloak 鉴权中心承担入口鉴权。
2. 微服务必须能解析和响应身份信息。
3. 微服务必须支持内部可信应用不经过 Gateway 的安全访问模型。
4. 接口级权限是当前基线能力，需保留后续扩展空间。
5. 字段级加密由独立 `field-encryption-starter` 提供。

## 14. 可观测与日志要求

1. Logging 与 Observability 保持独立。
2. Logging 为强制基础能力，不依赖 Observability。
3. Observability 提供指标、链路追踪、Prometheus、OTLP 与 Grafana 接入能力。
4. 日志、指标、Trace 必须支持 RequestId/TraceId 关联。
5. 日志必须支持脱敏和日志风暴保护。

## 15. Demo 与验收要求

1. Demo 为独立工程，位于 `Microservice/Microservice Demo/SourceCode`。
2. Demo 必须通过 Controller API 验证各 starter 对应用开发者暴露的能力。
3. Demo Reports 只保留 JMeter 脚本和最新中英文压测报告。
4. Demo 作为框架能力组合的可执行验收层。
5. 性能基线以 JMeter 脚本和压测报告形式维护。

## 16. 开源维护要求

1. 项目必须保持通用框架命名，不包含公司专属信息。
2. 所有 Markdown 文档必须标明作者 `Andy Yang`。
3. README 必须提供中英文版本。
4. 社区可通过 Issue、Discussion、Pull Request、文档改进、测试补充和能力扩展共同维护项目。
5. 捐赠支持可作为社区协作补充，但不得影响框架开放治理原则。

## 17. 产品边界

1. Framework 提供应用基座和工程契约，不替代业务领域建模、容量规划、生产拓扑设计、灾备方案或安全评审。
2. Gateway、Keycloak 服务端、Kubernetes 集群和各中间件服务属于部署环境，不由 Starter 创建或托管。
3. 高并发能力通过有界资源、连接复用、异步治理、缓存和可观测机制支撑；具体容量必须由目标环境中的性能测试证明。
4. Starter 只承诺已发布公共 API、配置契约和验收用例覆盖的能力，不把规划能力描述为当前实现。
5. 涉及缓存一致性、分布式事务、消息幂等和数据补偿的业务策略，必须由应用结合领域语义完成，Framework 提供标准机制和扩展点。
