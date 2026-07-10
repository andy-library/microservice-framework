# 统一工程与测试基线

Author: Andy Yang

## 1. Parent 真实基线

| 项目 | 已锁定值 |
| --- | --- |
| Java | 21；`maven.compiler.release=21` |
| Maven | 3.9.0 及以上 |
| Spring Boot | 3.3.13 |
| Spring Cloud | 2023.0.6 |
| GroupId / 根包 | `com.microservice.framework` |
| 配置前缀 | `framework.*` |
| 单元测试 | JUnit 5、AssertJ、Mockito、Surefire 3.2.5 |
| 集成测试 | Failsafe 3.2.5、Spring Boot Test、Testcontainers |
| 消费方契约 | Maven Invoker 3.7.0 |
| 构建治理 | Enforcer 3.4.1、Extra Enforcer Rules 1.8.0、Flatten 1.6.0 |

第三方版本只允许由 `microservice-framework-dependencies` 管理。Spring Boot/Cloud 已管理的依赖沿用其 BOM；未管理组件必须先在 Dependencies BOM 增加明确版本属性和坐标，再允许 Starter 使用。

## 2. Starter 标准工程结构

每个 Starter 是 `SourceCodes/microservice-framework-<name>-starter` 下的独立 Maven JAR 项目，继承 `microservice-framework-parent`，导入 Dependencies BOM 与 Framework BOM，不打可执行 Fat JAR。

```text
src/main/java/com/microservice/framework/<name>/
├── api/                 # 应用可直接使用的稳定 API
├── autoconfigure/       # Spring Boot 3 自动配置
├── core/                # 默认实现
├── properties/          # @ConfigurationProperties
└── spi/                 # 受控扩展接口
src/main/resources/
├── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── META-INF/spring-configuration-metadata.json
src/test/java/           # 单元、自动配置和契约测试
```

仅确有价值时创建内部 `*-core` 或 `test-support` 制品；业务应用只直接选择正式 Starter。

## 3. 自动配置设计规则

1. 使用 `@AutoConfiguration` 与 Boot 3 imports 文件，禁止旧 `spring.factories` 自动配置方式。
2. 使用 `@ConfigurationProperties`，配置必须校验并生成 IDE 元数据。
3. 默认实现使用 `@ConditionalOnMissingBean` 允许受控替换。
4. 可选技术使用 `@ConditionalOnClass`；功能开关使用 `@ConditionalOnProperty`。
5. 缺失必要配置、安全冲突和互斥 Starter 必须启动失败并提供 `FailureAnalyzer`。
6. 自动配置类按能力拆分，禁止单一超大配置类。
7. 公共 API 不暴露非必要第三方类型，避免锁死实现。

## 4. 依赖与技术栈规则

- `common-starter` 是最低层；其他 Starter 不得形成循环依赖。
- Logging 不依赖 Observability；Observability 可依赖 Logging。
- Nacos 与 Apollo、Jackson 与 Fastjson2 实现必须互斥。
- 可选集成依赖声明为 optional，并有“不存在该依赖”的启动测试。
- Starter POM 不声明第三方版本；所有版本来自 Dependencies BOM。
- 禁止传递引入无关 Web、Actuator、数据库或中间件客户端。
- Lombok 只作为编译辅助，不得成为公共 API 契约。

## 5. 测试分层

| 层级 | 工具 | 目标 |
| --- | --- | --- |
| 单元测试 | JUnit 5、AssertJ、Mockito | 算法、边界、异常和线程安全 |
| 自动配置测试 | `ApplicationContextRunner` | 启用、禁用、缺失依赖、非法配置、用户覆盖 |
| 架构契约 | Maven Enforcer、依赖树、必要时 ArchUnit | 依赖方向、包边界、禁止项 |
| 集成测试 | Spring Boot Test、Testcontainers | 与真实中间件协议交互 |
| 消费方契约 | Maven Invoker 最小应用 | 独立 Maven 消费、无版本引用和组合行为 |
| 故障测试 | Toxiproxy/Testcontainers 或可控替身 | 超时、中断、恢复、重试、降级 |
| 性能测试 | JMH 微基准、受控压测应用 | 热路径开销、容量边界和退化行为 |

## 6. 统一测试规则

1. 测试按 TDD 编写；修复缺陷必须先增加可复现失败测试。
2. `*Test` 由 Surefire 执行，`*IT` 由 Failsafe 在 `verify` 阶段执行。
3. 集成测试使用固定版本容器或受控企业测试环境，禁止依赖开发者本机服务。
4. 每个 Starter 至少提供一个最小消费方 Fixture；组合 Starter 提供组合 Fixture。
5. 并发测试必须具有重复运行稳定性，不能依赖任意 sleep 判断结果。
6. 安全测试验证失败关闭和敏感信息不泄漏。
7. 性能门槛在首个实现基准中固化，后续构建不得出现未经批准的明显退化。
8. `mvn clean verify` 必须运行该组件全部自动化门禁。

## 7. Dependencies BOM 待纳管技术

下列技术已由组件设计选定，但当前 Parent 尚未全部纳管。开发对应 Starter 前必须先在 Dependencies BOM 锁定与 Java 21、Boot 3.3.13、Cloud 2023.0.6 兼容的批准版本：

- Spring Cloud Alibaba Nacos Config、Apollo Client。
- Apache ShardingSphere-JDBC、数据库驱动与迁移工具。
- Elasticsearch Java API Client、XXL-JOB Executor、Drools/KIE。
- AWS SDK v2 S3、Testcontainers 模块、Awaitility、ArchUnit、JMH、Toxiproxy。

版本升级必须通过 Parent 依赖收敛、Starter 独立测试和真实组合应用回归。
