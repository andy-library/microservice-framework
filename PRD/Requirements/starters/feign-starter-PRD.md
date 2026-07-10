# Feign Starter PRD

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 制品 | `microservice-framework-feign-starter` |
| 引入策略 | 微服务远程调用按需显式引入 |
| 单体兼容 | 未声明 Feign Client 时不得产生远程调用副作用 |

## 1. 定位与边界

Feign 提供微服务间 HTTP API 调用的连接、超时、身份、上下文、重试、熔断和隔离治理。内部服务默认通过 Kubernetes DNS 寻址。

单体应用可引入本组件但不声明 Client；此时不得要求服务发现或创建无用连接。组件不负责 Gateway 调用，也不允许远程失败返回伪成功。

## 2. 技术栈与依赖

- Spring Cloud OpenFeign、受治理 HTTP Client、Resilience4j。
- 依赖 Common、JSON、Logging；Security 与 Observability 可选接入。

## 3. 核心能力

1. 服务地址、连接池、超时、请求预算和错误分类。
2. 服务身份、用户身份上下文和请求 ID 传播。
3. 服务 Token 缓存与异步刷新，避免逐次鉴权请求。
4. 仅幂等调用可用的有界重试、熔断、隔离和降级扩展。
5. Client 契约、Header 白名单和敏感数据保护。

## 4. 实现要求

- 每个 Client 必须显式声明服务名、超时和幂等属性。
- 用户 Token 只在确需用户语义的链路传播；服务间默认使用服务身份。
- Token 本地缓存并提前异步刷新，刷新失败使用未过期 Token；过期后失败关闭。
- 重试只能用于明确幂等且错误可重试的调用，并受总请求预算限制。
- 降级不得伪造成功；必须返回明确降级结果或错误。

## 5. 配置与扩展

- `framework.feign.clients.<name>.*`
- `framework.feign.security.*`
- `framework.feign.retry.*`
- `framework.feign.circuit-breaker.*`
- SPI：身份提供者、Header 传播器、错误解码器和受控降级。

## 6. 验收标准与方法

| 场景 | 验收方法 | 通过条件 |
| --- | --- | --- |
| 单体应用 | 引入 Starter 但不声明 Client | 应用正常启动，无发现/远程依赖要求 |
| 身份传播 | 调用服务身份与用户链路 | 身份按策略传播，无多余鉴权请求 |
| Token 刷新 | 模拟临近过期、刷新失败和过期 | 异步刷新，未过期可用，过期失败关闭 |
| 超时与预算 | 构造慢下游和重试 | 总耗时不超过预算 |
| 非幂等调用 | 为非幂等请求配置重试 | 配置被拒绝或重试不生效 |
| 熔断与降级 | 持续制造下游失败 | 熔断生效，结果不伪装成功 |

## 7. 交付物

自动配置、Client 开发规范、身份传播契约、韧性策略、故障演练和验证应用。
