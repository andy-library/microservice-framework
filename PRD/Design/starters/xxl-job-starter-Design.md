# XXL-JOB Starter 技术与测试设计

Author: Andy Yang

对应需求：[XXL-JOB PRD](../../PRD/starters/xxl-job-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、XXL-JOB Executor；Async 与 Observability optional。XXL-JOB 版本由 Dependencies BOM 锁定。

## 2. 技术设计

- 自动配置：Executor、Security、TaskGovernance、Lifecycle。
- 公共 API：`FrameworkJobHandler`、`JobContext`、`JobResult`、`JobIdempotencyGuard`、`JobFailureClassifier`。
- 任务模板在调用业务处理前执行参数校验、幂等、并发策略、上下文建立和超时控制。
- 执行结果严格区分成功、可重试失败、不可重试失败和取消。
- 手工运维任务使用专门标记、范围参数、Security 权限和 Audit 扩展事件。
- 停机先取消注册/拒绝新任务，再处理运行中任务。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 自动配置 | 注册参数、安全配置、缺失管理端、用户扩展 |
| 集成 | 真实/容器化管理端触发、回调和日志 |
| 可靠性 | 重复触发、并发策略、超时、重试、进程终止 |
| 安全 | 未授权执行、参数大小、敏感日志和运维审计 |
| 组合 | 可选 Async 上下文、Observability、Audit |
| 性能 | 高任务量注册和短任务执行开销 |

通过条件：重复触发不重复生效；状态真实；停机后任务具备恢复机会。
