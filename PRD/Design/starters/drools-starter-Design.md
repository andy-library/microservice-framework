# Drools Starter 技术与测试设计

Author: Andy Yang

对应需求：[Drools PRD](../../Requirements/starters/drools-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Drools/KIE、Spring Boot Autoconfigure；配置 Starter 与 Observability optional。Drools/KIE 版本必须由 Dependencies BOM 锁定并验证 Java 21。

## 2. 技术设计

- 自动配置：RuleEngine、RuleSource、DynamicRefresh、Metrics。
- 公共 API：`RuleSetId`、`RuleVersion`、`RuleRequest`、`RuleResult`、`RuleExecutor`、`RuleSource`、`RuleReleaseValidator`、`RuleVersionRouter`。
- 每个版本编译为不可变 KIE 容器；新版本先校验和预热，再以原子引用切换。
- 执行请求在开始时固定版本，进行中请求不受刷新影响；失败保留最后有效版本。
- 规则事实对象使用允许列表；规则执行禁止任意网络/文件 I/O。
- 灰度仅提供 `RuleVersionRouter` SPI，由业务或平台扩展决定路由。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 单元 | 版本、路由、输入输出、错误分类和执行超时 |
| 自动配置 | 规则来源、动态刷新开关、用户执行器覆盖 |
| 集成 | 合法/非法 DRL 编译、并发执行、原子切换、回滚 |
| 安全 | 未允许事实类型、危险表达式、敏感输入日志 |
| 并发 | 刷新期间旧/新版本隔离，高并发执行稳定性 |
| 性能 | 编译预热、单次执行和规则规模基准 |

通过条件：失败版本不激活；每次执行版本可追踪；刷新不破坏进行中请求。
