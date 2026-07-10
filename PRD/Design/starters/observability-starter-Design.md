# Observability Starter 技术与测试设计

Author: Andy Yang

对应需求：[Observability PRD](../../PRD/starters/observability-starter-PRD.md)

## 1. 技术栈

Logging、Spring Boot Actuator、Micrometer、Micrometer Tracing `1.4.1`、OpenTelemetry `1.35.0`、OTLP Exporter、Prometheus Registry、Spring AOP；Web optional。

## 2. 技术设计

- 重构现有代码，保留 Trace 注解、Baggage、Prometheus、健康指示器；移除 Gateway 专属能力。
- 自动配置拆分为 `Metrics`、`Tracing`、`OtlpExporter`、`Health`、`MdcBridge`。
- `MdcBridge` 把 Trace 上下文写入 Common/Logging 约定键，确保依赖方向单向。
- 指标定义 `FrameworkMeterBinder` SPI；标签过滤器拒绝高基数和敏感标签。
- OTLP 使用有界批处理、短超时和失败计量，导出线程与业务线程隔离。
- Liveness 只表达进程生存；Readiness 表达是否接收流量；依赖健康不随意影响 Liveness。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 自动配置 | Web/非 Web、OTLP 开关、Prometheus 开关、用户 Bean 覆盖 |
| 单元 | Trace 注解、标签过滤、健康状态语义、MDC 桥接 |
| 集成 | OTLP Collector/替身、Prometheus 抓取、跨线程 Trace |
| 故障 | Collector 中断、队列满、导出超时、恢复 |
| 安全 | 高基数和敏感标签拒绝；Trace/指标无敏感值 |
| 回归 | 迁移现有 79 个测试，删除 Gateway 假设，补充 Logging 单向依赖检查 |

通过条件：导出故障不阻塞业务；探针语义正确；无 Gateway 依赖；现有适用能力回归通过。
