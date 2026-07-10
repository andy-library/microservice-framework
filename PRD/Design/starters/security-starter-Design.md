# Security Starter 技术与测试设计

Author: Andy Yang

对应需求：[Security PRD](../../PRD/starters/security-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Spring Security、OAuth 2.0 Resource Server、Nimbus JOSE JWT；Web/Feign optional。版本沿用 Spring Boot/Spring Security BOM。

## 2. 技术设计

- 自动配置：ResourceServer、Authorization、ServiceIdentity、TrustedInternal。
- 公共 API：`FrameworkPrincipal`、`ServiceIdentity`、`PermissionEvaluator`、`SecurityEventPublisher`。
- JWT 本地校验 Issuer、Audience、签名、时间和允许算法；JWK 缓存与受控刷新。
- 默认拒绝全部未声明入口；公开接口通过明确注解/配置允许列表。
- Feign 使用 `ServiceTokenProvider` 获取 Client Credentials Token；不在每次调用重新请求未过期 Token。
- 受信内部应用仍使用服务凭据并结合网络边界；禁止匿名信任 Header。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| Security Slice | 合法/过期/错误签名、Issuer、Audience、权限 |
| 自动配置 | Web/非 Web、公开接口、默认拒绝、用户扩展 |
| 集成 | WireMock Keycloak/JWK；Key 轮换、缓存、中心中断 |
| 服务身份 | Token 缓存、异步刷新、并发刷新合并、过期失败关闭 |
| 安全 | 算法混淆、伪造 Header、Token/密钥泄漏扫描 |
| 性能 | JWT 本地校验与 Token 缓存高并发基准 |

通过条件：所有无效身份失败关闭；Keycloak 中断不导致逐请求远程校验或绕过认证。
