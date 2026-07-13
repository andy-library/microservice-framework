# Web Starter 技术与测试设计

Author: Andy Yang

对应需求：[Web PRD](../../Requirements/starters/web-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Spring MVC、Jakarta Validation、Springdoc OpenAPI；Security 与 Observability optional/default combination。Springdoc 版本由 Dependencies BOM 管理。

## 2. 技术设计

- 公共 API：`ApiResponse<T>`、`ApiError`、`FrameworkException`、`ExceptionMapper`、`ResponseExceptionPolicy`。
- 自动配置：Response、Exception、Validation、RequestContext、OpenAPI、GracefulShutdown。
- `@RestControllerAdvice` 映射校验、业务、系统和依赖异常；HTTP 状态保持真实语义。
- 统一响应默认应用；例外由显式注解和允许列表控制，并暴露扫描结果。
- Filter 建立 RequestId/上下文并在 finally 清理；请求/Header/载荷上限由容器和框架共同校验。
- OpenAPI 仅描述协议，内部接口通过分组和安全标记隔离。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| MVC Slice | 成功、各类异常、校验、分页和响应例外 |
| 自动配置 | 无 Security/Gateway、可选组合、用户 Mapper 覆盖 |
| 安全 | 超大/恶意输入、异常脱敏、内部接口文档隔离 |
| 集成 | 真实端口请求、RequestId、OpenAPI、优雅停机 |
| 契约 | 统一响应与 HTTP 状态消费方契约 |
| 性能 | 过滤器、异常映射和 JSON 响应热路径开销 |

通过条件：失败不返回 HTTP 200；上下文不泄漏；无 Gateway 仍能安全组合运行。
