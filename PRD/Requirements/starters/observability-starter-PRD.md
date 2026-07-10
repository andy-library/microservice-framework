# Observability Starter PRD

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 制品 | `microservice-framework-observability-starter` |
| 引入策略 | 生产默认推荐、按需显式引入 |
| 定位 | 指标、链路追踪、健康诊断和告警接入 |

## 1. 定位与边界

Observability 为生产应用提供统一的可观测能力。未引入时，应用必须通过架构验收证明具有兼容的健康探针和故障诊断能力。

组件可以依赖 Logging，但 Logging 不得反向依赖本组件。组件不建设 Prometheus、Grafana、Collector 或告警平台。

## 2. 技术栈与依赖

- Spring Boot Actuator、Micrometer、Micrometer Tracing。
- Prometheus 指标导出、OpenTelemetry/OTLP 链路导出。
- Kubernetes Liveness、Readiness、Startup Probe。
- 依赖 Common、JSON、Logging；Web 能力保持可选。

## 3. 核心能力

1. JVM、线程池、连接池、HTTP、数据库及 Starter 自定义指标。
2. TraceId、SpanId 创建与传播；采样和敏感属性治理。
3. 健康探针、依赖健康指示器、启动与就绪状态。
4. 告警所需指标、异常事件和 Grafana 接入说明。
5. 导出端故障隔离、缓冲边界、丢弃计量和降级。

## 4. 实现要求

- 指标标签必须受控，禁止用户 ID、订单 ID 等高基数字段。
- 健康检查区分存活与就绪；外部依赖短暂失败不得错误触发进程反复重启。
- 遥测导出不得阻塞核心业务；队列、批量、超时和重试必须有界。
- Trace 和指标不得携带 Token、密钥及敏感业务数据。
- 未引入 Web 时非 Web 应用仍可启动并使用适用能力。

## 5. 配置与扩展

- `framework.observability.metrics.*`
- `framework.observability.tracing.*`
- `framework.observability.otlp.*`
- `framework.observability.health.*`
- SPI：业务指标注册、健康指示器、标签过滤器、采样策略。

## 6. 验收标准与方法

| 场景 | 验收方法 | 通过条件 |
| --- | --- | --- |
| 指标导出 | 运行验证应用并抓取 Prometheus | 必需指标存在，标签符合治理规则 |
| 链路传播 | 跨线程和模拟远程调用 | Trace 上下文连续且不串扰 |
| 健康探针 | 模拟启动、就绪、依赖故障 | 三类状态语义正确 |
| 导出端中断 | 断开 OTLP/指标后端并压测 | 核心请求不被阻塞，失败可计量告警 |
| 高基数防护 | 注册非法标签 | 被拒绝或裁剪并给出诊断 |
| 可选组合 | 分别在 Web 与非 Web 应用启动 | 自动配置按条件生效且均通过 |
| 敏感数据 | 扫描 Trace、指标和日志 | 无敏感明文 |

## 7. 交付物

自动配置、仪表盘与告警接入规范、健康探针说明、指标字典、故障演练和验证应用。
