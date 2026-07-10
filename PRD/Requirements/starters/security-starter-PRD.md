# Security Starter PRD

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 制品 | `microservice-framework-security-starter` |
| 引入策略 | Web 默认组合，其他应用按需 |
| 权限范围 | V1 接口级权限，保留扩展能力 |

## 1. 定位与边界

Security 统一外部用户身份、内部服务身份、受信内部应用基础鉴权和接口权限。Gateway 使用 Keycloak 鉴权；微服务入口必须自行验证身份，不能仅信任 Gateway。

组件不建设 Keycloak 或 Gateway，不提供多租户能力。

## 2. 技术栈与依赖

- Spring Security、OAuth 2.0 Resource Server、Keycloak 标准 JWT/JWK。
- 依赖 Common、JSON、Logging；Web 与 Feign 可选组合。

## 3. 核心能力

1. JWT 本地校验、JWK 缓存刷新、Issuer/Audience/过期校验。
2. 服务身份 Client Credentials 与 Feign 集成契约。
3. 受信内部应用的基础鉴权、网络边界与凭据轮换。
4. 接口级权限、公开接口显式声明和默认拒绝。
5. 安全失败分类、审计事件和敏感信息保护。

## 4. 实现要求

- 每个受保护入口都必须在服务内验证 Token；禁止仅依赖来源 Header。
- JWK 使用缓存并异步刷新；未知 Key 可触发一次受控刷新，校验失败必须拒绝。
- 受信内部应用必须使用明确服务身份和受控网络，不得完全匿名访问。
- 默认拒绝未声明接口；公开接口必须显式标记。
- Token、密钥和完整身份凭据不得写入日志、Trace 或错误响应。

## 5. 配置与扩展

- `framework.security.resource-server.*`
- `framework.security.service-identity.*`
- `framework.security.trusted-internal.*`
- `framework.security.authorization.*`
- SPI：权限解析、服务凭据提供者和安全事件监听器。

## 6. 验收标准与方法

| 场景 | 验收方法 | 通过条件 |
| --- | --- | --- |
| JWT 校验 | 使用合法、过期、错误 Issuer/Audience Token | 仅合法 Token 通过 |
| JWK 轮换 | 轮换签名 Key 并模拟中心短暂不可用 | 缓存与刷新符合策略，不绕过校验 |
| 服务身份 | 服务间调用受保护接口 | 本地验证通过，性能无逐次远程鉴权开销 |
| 受信内部应用 | 直接访问无 Gateway 入口 | 使用基础鉴权通过，匿名访问拒绝 |
| 默认拒绝 | 添加未声明接口 | 默认不可访问 |
| 敏感保护 | 扫描日志、Trace 和错误响应 | 无 Token 和密钥明文 |

## 7. 交付物

自动配置、安全注解与契约、Keycloak 接入说明、内部访问规范、威胁测试和验证应用。
