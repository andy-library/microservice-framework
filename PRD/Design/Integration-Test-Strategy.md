# 集成与应用验收策略

Author: Andy Yang

本文定义 Framework 对真实协议、Starter 组合和应用侧公共能力的持续验收方式。测试结果以 CI 记录和 `Microservice Demo/Reports` 中的正式报告为准。

## 1. 测试分层

| 层级 | 运行位置 | 验证目标 |
| --- | --- | --- |
| 单元与契约测试 | 各 Parent/Starter 模块 | 公共 API、算法、配置绑定、自动配置条件和失败边界 |
| Starter 集成测试 | 对应 Starter | 与框架适配层、序列化、Spring 容器和外部协议的集成 |
| Demo 嵌入式验收 | Demo 默认 Profile | 所有面向应用开发者的 Controller API 和跨 Starter 组合 |
| Demo 真实中间件验收 | `real-middleware-acceptance` Profile | MySQL、Redis、Kafka、Elasticsearch、Nacos、Apollo 等真实服务交互 |
| 性能测试 | JMeter | 在声明的环境、数据和并发模型下形成可复现基线 |

## 2. 默认构建

每个模块的默认命令为：

```bash
mvn clean verify
```

默认构建必须可重复、不得依赖开发者机器上的私有服务或凭证。需要外部服务的用例应使用独立 Profile，并通过环境变量或 Maven 属性注入连接信息。

## 3. Demo 验收

Demo 为独立可运行应用。每个 Starter 对应用开发者开放的能力必须由独立 Controller 提供测试 API，并由自动化测试覆盖正常、边界和失败场景。Starter 内部实现细节不得为了测试而暴露为 HTTP API。

真实中间件验收命令：

```bash
mvn verify -Preal-middleware-acceptance \
  -Ddemo.real.middleware.acceptance=true
```

连接地址和凭证必须由环境注入，不得提交真实密码、Token 或私有网络信息。测试结束后应清理临时主题、索引、键、对象和数据库记录，或使用隔离命名空间避免污染共享环境。

## 4. 中间件覆盖

| 能力 | 最低真实验收 |
| --- | --- |
| Database | 连接、事务、读写路由、迁移边界及显式启用的增强能力 |
| Redis | 序列化、TTL、失效、锁、限流、计数和批量边界 |
| Kafka | 生产、消费、Header、提交策略、重试、死信和幂等边界 |
| Elasticsearch | 索引、文档、查询、分页、批量和别名操作 |
| Nacos/Apollo | 配置读取、优先级、刷新、删除/恢复、互斥和敏感信息保护 |
| Object Storage | 上传、下载、删除、元数据、预签名和大对象边界 |
| Security/Feign | JWT/服务身份校验、上下文传播、超时、重试和失败关闭 |

## 5. 通过标准

1. Parent、全部 Starter 和 Demo 的默认 `mvn clean verify` 通过。
2. 真实中间件 Profile 在声明的受支持环境中通过，且不存在无理由跳过的验收项。
3. 每个公共能力均能从应用代码调用，并有可定位到 PRD 条目的测试证据。
4. 失败场景不会泄露敏感信息，不产生无界重试、无限队列或资源泄漏。
5. 测试可重复执行，不依赖执行顺序，并能清理或隔离测试数据。
6. 性能报告必须记录硬件、JVM、数据规模、并发模型、持续时间、成功率、吞吐和延迟分位数；不得将单一环境结果宣称为通用容量保证。
