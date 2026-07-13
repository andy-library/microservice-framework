# Logging Starter 技术与测试设计

Author: Andy Yang

对应需求：[Logging PRD](../../Requirements/starters/logging-starter-PRD.md)

## 1. 技术栈

Common、JSON、SLF4J、Logback、Logstash Logback Encoder `7.4`、Janino。移除当前 Observability、Actuator、Web 和 Spring Cloud Context 硬依赖。

## 2. 技术设计

- 保留并重构现有 `masking`、`filter`、`properties` 与结构化日志 API。
- `LoggingBaseAutoConfiguration` 只配置字段、上下文桥接和生命周期；不提供 Actuator 端点。
- `MaskingLoggingAutoConfiguration`、`FloodProtectionAutoConfiguration`、`AsyncLoggingAutoConfiguration` 独立条件启用。
- TraceId/SpanId 通过 Common 上下文键或可选桥接读取，不引用 Observability 类型。
- 异步 Appender 使用有界队列；高等级日志采用同步降级；关闭时限时刷盘并计数未刷盘事件。
- 动态级别定义 `LoggingLevelController` SPI，由配置 Starter 或运维集成实现。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 单元 | 脱敏规则、结构化字段、采样、限流、异常格式 |
| 自动配置 | 无 Observability/Web 启动；各能力开关；用户 Bean 覆盖 |
| 并发/故障 | 队列满、日志风暴、Appender 异常、优雅停机刷盘 |
| 安全 | Token、密钥、证件号、异常和嵌套 JSON 脱敏 |
| 架构 | 依赖树禁止 Observability、Actuator、Web、配置中心 |
| 回归 | 迁移现有 98 个测试并补充独立运行与依赖边界测试 |

通过条件：独立运行；敏感明文零泄漏；过载不无限阻塞业务线程；当前能力回归通过。
