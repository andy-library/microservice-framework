# Logging Starter PRD

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 制品 | `microservice-framework-logging-starter` |
| 引入策略 | 所有应用强制显式引入 |
| 核心边界 | 不依赖 Observability、Actuator、Web 或配置中心 |

## 1. 定位与边界

Logging 提供任何应用都必须具备的轻量、可靠、结构化日志能力。它消费 Common 轻量上下文；引入 Observability 时可额外输出 TraceId 和 SpanId，但自身必须独立工作。

Logging 不负责指标、链路追踪、健康检查和日志采集平台。

## 2. 技术栈与依赖

- SLF4J API、Logback 默认实现、JSON Encoder。
- 依赖 Common 和 JSON。
- 通过可选桥接读取追踪上下文，不直接依赖 Observability。

## 3. 核心能力

1. 统一结构化日志字段、时间、级别、服务、环境、请求 ID 和异常格式。
2. 敏感字段脱敏、禁止项检测和安全默认值。
3. 异步日志、队列容量、丢弃策略、降级和优雅停机刷盘。
4. 日志风暴保护、采样、单条日志大小限制和重复异常抑制。
5. 动态日志级别扩展接口及变更审计。

## 4. 实现要求

- 默认输出机器可解析 JSON；本地开发可显式选择可读格式。
- 密码、Token、密钥、完整证件号等禁止写入日志；异常和请求体也必须脱敏。
- 异步队列满时不得无限阻塞业务线程；高等级日志必须采用受控降级策略并报警。
- 应用停机时在限定时间内刷新日志；超时必须记录丢失数量。
- 日志配置失败不得静默退回不受治理配置。

## 5. 配置与扩展

- `framework.logging.format=json|console`
- `framework.logging.async.enabled`
- `framework.logging.async.queue-capacity`
- `framework.logging.masking.*`
- `framework.logging.flood-protection.*`
- SPI：脱敏规则、附加结构化字段、动态级别提供者。

## 6. 验收标准与方法

| 场景 | 验收方法 | 通过条件 |
| --- | --- | --- |
| 独立运行 | 不引入 Observability/Web 启动应用 | 日志完整输出，依赖树无重型组件 |
| 结构化字段 | 触发正常、错误和上下文日志 | 必需字段齐全且可解析 |
| 脱敏 | 写入各类敏感值与异常 | 输出中无原始敏感值 |
| 队列过载 | 压测制造日志风暴 | 业务线程不被无限阻塞，丢弃可计量告警 |
| 优雅停机 | 写入日志后终止应用 | 限时刷盘，丢失情况可诊断 |
| 可选追踪 | 分别引入和不引入 Observability | 两种组合均启动；引入时包含 TraceId/SpanId |
| 动态级别 | 通过扩展接口变更级别 | 生效、可回滚并留下审计记录 |

## 7. 交付物

默认日志配置、自动配置、脱敏规则、运行手册、压力基准、契约测试和验证应用。
