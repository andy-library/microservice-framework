# Microservice Framework Web Starter

Author: Andy Yang

Web Starter — 提供 HTTP 入口、统一响应、全局异常处理、参数校验、RequestId 和 OpenAPI 文档。

## 核心能力

| 能力 | API / 类 | 说明 |
| --- | --- | --- |
| 统一响应 | `ApiResponse` / `PageResponse` / `ErrorCodeResponse` | 统一 JSON 响应结构，支持成功码、错误码、时间戳和请求 ID |
| 全局异常处理 | `GlobalExceptionHandler` | 自动捕获 FrameworkException、ValidationException 等，转换为统一错误响应 |
| 请求上下文 | `RequestIdContext` / `RequestIdFilter` | 请求 ID 传播与生成，默认从 `X-Request-ID` 头读取或自动生成 |
| 参数校验 | `spring-boot-starter-validation` | Jakarta Validation 集成，自动校验请求参数 |
| OpenAPI 文档 | `OpenApiProperties` | Swagger/OpenAPI 文档生成，可按需启用 |

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-web-starter</artifactId>
</dependency>
```

版本由 `microservice-framework-bom` 统一管理，无需指定。

### 2. 最小配置

```yaml
framework:
  web:
    response:
      success-code: 0               # 成功响应默认 code
      error-code: -1                 # 错误响应默认 code
      include-timestamp: true        # 是否包含时间戳
      include-request-id: true       # 是否包含请求 ID
    exception:
      enabled: true                  # 是否启用全局异常处理
      include-stack-trace: false     # 是否包含堆栈信息（仅开发环境）
    request-id:
      enabled: true                  # 是否启用请求 ID 过滤器
      header-name: "X-Request-ID"    # 请求 ID 头名称
      generate-if-missing: true      # 头缺失时是否自动生成
    open-api:
      enabled: false                 # 是否启用 OpenAPI 文档
      title: "My API"                # API 文档标题
      version: "1.0.0"               # API 文档版本
```

### 3. 使用示例

```java
@GetMapping("/users/{id}")
public ApiResponse<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return ApiResponse.success(user);
}
```

## 配置参考

所有配置前缀为 `framework.web`。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `response.success-code` | `0` | 成功响应默认 code |
| `response.error-code` | `-1` | 错误响应默认 code |
| `response.include-timestamp` | `true` | 是否在响应中包含时间戳 |
| `response.include-request-id` | `true` | 是否在响应中包含请求 ID |
| `exception.enabled` | `true` | 是否启用全局异常处理 |
| `exception.include-stack-trace` | `false` | 是否在错误响应中包含堆栈信息 |
| `request-id.enabled` | `true` | 是否启用请求 ID 过滤器 |
| `request-id.header-name` | `X-Request-ID` | 请求 ID 头名称 |
| `request-id.generate-if-missing` | `true` | 头缺失时是否自动生成请求 ID |
| `open-api.enabled` | `false` | 是否启用 OpenAPI 文档生成 |
| `open-api.title` | — | API 文档标题 |
| `open-api.version` | — | API 文档版本 |

## 自动注册 Bean

仅在 SERVLET Web 应用中且 `framework.web.enabled=true` 时激活：

| 自动配置类 | 注册 Bean / 功能 |
| --- | --- |
| `WebAutoConfiguration` | `WebProperties` Bean |
| `ExceptionHandlerAutoConfiguration` | `GlobalExceptionHandler` |
| `RequestIdAutoConfiguration` | `RequestIdFilter` + `RequestIdContext` |

用户可通过注册自定义同类型 Bean 覆盖默认实现。

## 集成测试策略

| 状态 | 说明 |
| --- | --- |
| 当前覆盖 | MockMvc 测试覆盖异常处理、请求上下文传播、响应结构、属性绑定 |
| 真实中间件 | 无需外部中间件 |
| 计划方案 | MockMvc 测试已完整覆盖 Web 层行为，无需真实中间件集成测试 |
| 执行方式 | Failsafe + `@Tag("integration")` profile，不在默认 `mvn clean verify` 中 |
| 补齐时间 | 当前覆盖已完整 |

当前 `mvn clean verify` 仍为默认准入门禁。Web Starter 不依赖外部中间件，MockMvc 测试已覆盖所有核心行为。
