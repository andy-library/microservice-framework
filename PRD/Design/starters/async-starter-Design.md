# Async Starter 技术与测试设计

Author: Andy Yang

对应需求：[Async PRD](../../PRD/starters/async-starter-PRD.md)

## 1. 技术栈

Common、Logging、Java 21 Executor、Spring Task Execution、Micrometer optional。V1 使用平台线程池；虚拟线程作为后续经基准验证的实现选项。

## 2. 技术设计

- 公共 API：`ManagedExecutorRegistry`、`ManagedTask`、`TaskContextPropagator`、`TaskRejectionHandler`。
- 每个命名池由 `framework.async.executors.<name>` 配置核心线程、最大线程、队列、超时和拒绝策略。
- `TaskDecorator` 捕获 Common 上下文快照，执行后强制清理。
- 默认拒绝策略为明确失败；允许经配置选择 CallerRuns，但禁止静默丢弃。
- 生命周期组件在停机时停止接收、等待、取消并输出剩余任务统计。
- 提供治理检查器识别 Spring 容器内未受管 Executor Bean。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 单元 | 配置绑定、上下文传播/清理、异常和拒绝策略 |
| 并发 | 池隔离、队列满、取消、超时和大量并发提交 |
| 自动配置 | 多命名池、非法无界配置、用户扩展 |
| 生命周期 | 优雅停机完成/取消/统计 |
| 架构 | 禁止 Kafka/XXL-JOB 依赖；检测未受管池 |
| 性能 | 提交开销、上下文传播和不同池配置基准 |

通过条件：上下文零串扰；任何队列有界；拒绝和未完成任务均可感知。
