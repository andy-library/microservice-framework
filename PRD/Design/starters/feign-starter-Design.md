# Feign Starter 技术与测试设计

Author: Andy Yang

对应需求：[Feign PRD](../../PRD/starters/feign-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Spring Cloud OpenFeign、Spring Cloud LoadBalancer optional、Apache/OkHttp 受治理客户端、Resilience4j；Security/Observability optional。具体 HTTP Client 只允许一种并由 BOM 管理。

## 2. 技术设计

- 自动配置：ClientDefaults、Identity、ContextPropagation、Resilience、Metrics。
- 公共注解/API：`@FrameworkFeignClient`、`CallPolicy`、`ServiceTokenProvider`、`FeignErrorDecoder`、`FallbackResult`。
- 默认使用 Kubernetes DNS URL/服务名；Nacos 服务发现只在未来显式适配。
- 服务 Token 以过期时间为边界缓存并提前异步刷新；用户 Token 仅按 Client 策略传播。
- 每个 Client 声明连接/读取/总预算、幂等性和重试；总预算限制全部尝试。
- Fallback 必须返回显式降级类型或抛出异常，禁止伪造正常业务结果。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 自动配置 | 无 Client 单体启动、HTTP Client 互斥、默认策略 |
| 契约 | Header、错误解码、超时、幂等重试和 Fallback 语义 |
| 集成 | WireMock/MockWebServer 服务调用、连接池和 DNS 地址 |
| 安全 | 服务/用户身份传播、Token 刷新/过期、敏感输出 |
| 故障 | 慢响应、断连、熔断、半开、预算耗尽 |
| 性能 | Token 缓存命中、连接复用和并发调用开销 |

通过条件：单体无副作用；无逐次远程鉴权；超时总预算生效；降级不伪成功。
