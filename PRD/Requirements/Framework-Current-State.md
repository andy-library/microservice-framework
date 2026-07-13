# Microservice Framework 当前实现范围

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 文档版本 | v1.1 |
| 文档状态 | 当前公开基线 |
| 正式源码 | `SourceCode` |
| 验收应用 | `Microservice Demo/SourceCode` |

## 1. 已交付制品

当前仓库包含一套 Parent 工程、Dependencies BOM、Framework BOM、Starter Parent、19 个正式 Starter，以及一个独立 Demo 应用。正式组件清单与引入策略以 [组件目录](./Framework-Component-Catalog.md) 为准。

## 2. Starter 统一实现契约

每个 Starter 按其职责提供以下一种或多种能力：

- 面向应用开发者的公共 API、注解和值对象。
- 使用 `framework.*` 命名空间的类型安全配置。
- 基于 Spring Boot 自动配置机制的条件装配。
- 对缺失配置、冲突配置和不安全组合的启动期诊断。
- 允许应用在明确边界内覆盖默认 Bean 或选择实现。
- 覆盖公共契约、属性绑定、启停条件和关键失败边界的自动化测试。

具体公共能力和验收标准由各 [Starter PRD](./starters/README.md) 定义。

## 3. 构建治理

- Parent 不隐式引入运行时 Starter。
- 第三方依赖与 Maven 插件版本由 Parent 和 Dependencies BOM 集中治理。
- Framework BOM 管理正式 Starter 版本，应用无需逐个声明版本。
- 受保护版本、依赖收敛和禁止依赖规则通过 Maven Enforcer 与消费方契约验证。
- Starter 可独立构建和测试，Demo 用于验证跨 Starter 组合及应用侧调用方式。

## 4. 已实现边界

- Logging 与 Observability 独立；Logging 是基础能力，Observability 按运行需要引入。
- Nacos 与 Apollo 是互斥的配置中心方案，均可与 Kubernetes 配置来源组合。
- Database 提供读写分离、分库分表、幂等和 Outbox 等机制，其中增强能力按配置显式启用。
- Redis、Kafka、Elasticsearch、XXL-JOB、Drools 和对象存储按需引入。
- Web、Security 与 Feign 保持独立，可按应用形态组合。
- Audit 面向需要操作审计的应用引入；Field Encryption 面向敏感字段保护场景引入。

## 5. 验证入口

| 验证层 | 入口 | 目的 |
| --- | --- | --- |
| Parent 契约 | `SourceCode/microservice-framework-parent` | 验证版本、依赖和构建治理 |
| Starter 验证 | 各 Starter 的 `mvn clean verify` | 验证公共 API、配置和自动装配 |
| 组合验证 | `Microservice Demo/SourceCode/microservice-framework-demo` | 验证应用侧 Controller API 与跨组件组合 |
| 真实中间件验证 | Demo 的 `real-middleware-acceptance` Profile | 验证外部协议和真实服务交互 |
| 性能验证 | `Microservice Demo/Reports` | 保存可复现的 JMeter 脚本与报告 |

精确测试数量和最近一次执行结果属于测试报告与 CI 运行信息，不在 PRD 中固化，避免随代码演进失真。
