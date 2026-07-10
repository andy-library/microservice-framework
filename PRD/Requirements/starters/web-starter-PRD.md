# Web Starter PRD

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 制品 | `microservice-framework-web-starter` |
| 引入策略 | Web 应用默认显式引入 |
| 默认组合 | 默认与 Security Starter 组合，但保持独立 |

## 1. 定位与边界

Web 提供统一 HTTP 入口、响应、异常、校验、请求上下文和 API 文档治理。公网应用通过外部 Gateway 访问，Framework 不实现 Gateway；没有 Gateway 的内部入口仍必须遵守 Security 规则。

组件不承载业务 Controller，不隐藏业务异常，也不允许通过统一响应把失败包装成 HTTP 成功。

## 2. 技术栈与依赖

- Spring MVC、Jakarta Validation、OpenAPI 接入。
- 依赖 Common、JSON、Logging；Security 与 Observability 可选但为默认组合。

## 3. 核心能力

1. 统一响应结构、错误码、HTTP 状态和异常映射。
2. 请求 ID、Header、参数校验、载荷大小和输入安全治理。
3. Controller 边界、幂等键接口扩展和分页契约。
4. OpenAPI 文档、接口分组和内部接口标记。
5. 优雅停机、就绪状态联动和请求排空。

## 4. 实现要求

- 统一响应为强制默认；例外必须通过受控扩展并可扫描、审计。
- HTTP 状态必须表达真实结果，禁止失败返回 `200`。
- 校验失败、业务失败、系统失败和依赖失败必须使用不同错误类别。
- 请求体、Header 和上传大小必须有界；错误响应不得泄漏堆栈和敏感信息。
- Controller 只负责协议适配，不直接承载复杂业务逻辑。

## 5. 配置与扩展

- `framework.web.response.*`
- `framework.web.request.*`
- `framework.web.validation.*`
- `framework.web.openapi.*`
- SPI：统一响应例外、异常映射、请求上下文扩展。

## 6. 验收标准与方法

| 场景 | 验收方法 | 通过条件 |
| --- | --- | --- |
| 统一响应 | 调用成功及各类失败接口 | 结构一致，HTTP 状态与错误码正确 |
| 受控例外 | 注册与未注册例外响应 | 注册项生效并可审计，未注册项被阻断 |
| 输入治理 | 提交非法、超大和恶意输入 | 请求被正确拒绝且不泄漏信息 |
| 无 Gateway | 直接访问验证应用 | Web 正常工作，Security 组合规则仍可用 |
| OpenAPI | 生成接口文档 | 文档完整，内部/敏感接口按规则处理 |
| 优雅停机 | 持续请求时滚动终止 | 停止接收新流量并排空进行中请求 |

## 7. 交付物

自动配置、统一响应与异常 API、Web 开发规范、OpenAPI 规范、安全测试和验证应用。
