# Microservice Framework Security Starter

Author: Andy Yang

Security Starter — 提供 Keycloak JWT 认证、服务身份校验、接口权限控制、CORS 和路径安全规则。

## 核心能力

| 能力 | API 接口 | 说明 |
| --- | --- | --- |
| JWT 认证 | `JwtAutoConfiguration` | Keycloak JWT 验证，支持 issuer、JWKS、audience 和角色 claim 映射 |
| 服务身份校验 | `ServiceAuthenticator` | 服务间调用身份验证，通过 `X-Service-Token` 头标识调用来源 |
| 接口权限控制 | `PermissionEvaluator` | 从 JWT claims 提取权限列表（`resource:action` 格式），细粒度鉴权 |
| 安全上下文 | `SecurityContext` | 当前认证用户信息抽象 |
| CORS 跨域 | `FrameworkSecurityAutoConfiguration` | CORS 策略配置，支持源、方法和预检缓存 |
| 路径安全规则 | `FrameworkSecurityAutoConfiguration` | permit 路径（免认证）和 deny 路径（禁止访问）配置 |

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-security-starter</artifactId>
</dependency>
```

版本由 `microservice-framework-bom` 统一管理，无需指定。

按需额外引入：

```xml
<!-- Keycloak OAuth2 资源服务器模式（按需） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### 2. 最小配置

```yaml
framework:
  security:
    jwt:
      enabled: true                                  # 是否启用 JWT 认证
      issuer-uri: https://keycloak.example.com/realms/my-realm
      jwk-set-uri: https://keycloak.example.com/realms/my-realm/protocol/openid-connect/certs
      audience: my-service                           # audience 验证值
      claim-roles-key: "roles"                       # JWT 角色 claim key
    service-identity:
      enabled: true                                  # 是否启用服务身份验证
      service-token-header: "X-Service-Token"        # 服务 Token 头名称
    permission:
      enabled: true                                  # 是否启用权限检查
      permission-claim-key: "permissions"            # JWT 权限 claim key
    cors:
      enabled: true                                  # 是否启用 CORS
      allowed-origins:
        - "*"                                        # 允许的源（生产环境应配置具体域名）
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - PATCH
        - OPTIONS
      max-age: 3600                                  # 预检缓存时间（秒）
    path:
      permit-paths:
        - /actuator/**                               # 允许免认证访问的路径
        - /health
      deny-paths: []                                 # 禁止访问的路径
```

### 3. 使用示例

```java
@GetMapping("/admin/users")
@PreAuthorize("hasPermission('user:admin')")
public ApiResponse<List<User>> listAdminUsers() {
    return ApiResponse.success(userService.listAdmins());
}
```

## 配置参考

所有配置前缀为 `framework.security`。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `jwt.enabled` | `true` | 是否启用 JWT 认证 |
| `jwt.issuer-uri` | — | JWT issuer URI（Keycloak realm URL） |
| `jwt.jwk-set-uri` | — | JWK Set URI（Keycloak JWKS endpoint） |
| `jwt.audience` | — | JWT audience 验证值 |
| `jwt.claim-roles-key` | `roles` | JWT 角色 claim key |
| `service-identity.enabled` | `true` | 是否启用服务身份验证 |
| `service-identity.service-token-header` | `X-Service-Token` | 服务 Token 头名称 |
| `permission.enabled` | `true` | 是否启用权限检查 |
| `permission.permission-claim-key` | `permissions` | JWT 权限 claim key |
| `cors.enabled` | `true` | 是否启用 CORS |
| `cors.allowed-origins` | `["*"]` | 允许的源列表 |
| `cors.allowed-methods` | `["GET","POST","PUT","DELETE","PATCH","OPTIONS"]` | 允许的 HTTP 方法 |
| `cors.max-age` | `3600` | 预检缓存时间（秒） |
| `path.permit-paths` | `["/actuator/**","/health"]` | 免认证路径列表 |
| `path.deny-paths` | `[]` | 禁止访问路径列表 |

## 自动注册 Bean

当 Spring Security 在 classpath 上且 `framework.security.enabled=true` 时，自动注册：

| 自动配置类 | 注册 Bean | 条件 |
| --- | --- | --- |
| `FrameworkSecurityAutoConfiguration` | `SecurityFilterChain` | `@ConditionalOnMissingBean` |
| `JwtAutoConfiguration` | JWT 解析与验证组件 | `spring-boot-starter-oauth2-resource-server` 在 classpath |

配置在 Spring Boot 默认安全配置之前运行（`@AutoConfigureBefore`），以覆盖默认 SecurityFilterChain。

用户可通过注册自定义 `SecurityFilterChain` Bean 完全替换默认安全配置。

## 集成测试策略

| 状态 | 说明 |
| --- | --- |
| 当前覆盖 | JWT 解析与认证过滤器测试（MockMvc + mock JWKS）、属性绑定、自动配置条件 |
| 真实中间件 | 暂未包含 |
| 计划方案 | Keycloak 集成测试使用 Testcontainers Keycloak 模块，验证真实 JWT 签发/验证、角色映射、权限提取等需要真实 Keycloak 行为的场景；计划在后续迭代补齐 |
| 执行方式 | Failsafe + `@Tag("integration")` profile，不在默认 `mvn clean verify` 中 |
| 补齐时间 | 后续迭代（Keycloak 集成测试） |

当前 `mvn clean verify` 仍为默认准入门禁。真实中间件集成测试将在 Dependencies BOM 补齐对应 Testcontainers 模块后落地。
