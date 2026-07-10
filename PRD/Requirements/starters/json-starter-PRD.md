# JSON Starter PRD

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 制品 | `microservice-framework-json-starter` |
| 引入策略 | 所有应用强制显式引入 |
| 默认实现 | Jackson；Fastjson2 为构建期显式替代实现 |

## 1. 定位与边界

JSON Starter 为所有 Starter 和业务应用提供唯一公共 JSON 契约，消除各组件自行创建序列化器造成的日期、精度、兼容性和安全差异。

本组件不依赖 Web，不允许应用运行期间切换 JSON 实现，也不负责消息 Schema Registry。

## 2. 技术栈与依赖

- 公共接口：`JsonCodec`、`JsonTypeReference`、统一异常。
- 默认 Jackson，提供 Fastjson2 适配实现。
- 依赖 Common；两种实现必须互斥，并通过同一契约测试套件。

## 3. 核心能力

1. 对象、字符串、字节数组、泛型和集合的序列化与反序列化。
2. 统一日期时间、金额、枚举、空值、未知字段和命名策略。
3. 安全反序列化默认策略、最大深度和最大载荷限制。
4. 敏感类型扩展、模块注册和业务自定义序列化器扩展点。
5. 稳定的异常分类和脱敏错误信息。

## 4. 实现要求

- 默认激活 Jackson；选择 Fastjson2 必须通过明确 Maven 依赖或配置，并排除另一实现。
- 同时存在两种实现、没有实现或实现配置冲突时必须启动失败。
- 默认禁止危险多态反序列化和任意类型实例化。
- `LocalDateTime` 按 Common 时区规范处理；大整数、金额和精度不得静默丢失。
- 不允许业务直接创建不受治理的全局 `ObjectMapper` 或 Fastjson2 配置；允许通过受控 Customizer 扩展。

## 5. 配置与扩展

- `framework.json.provider=jackson|fastjson2`
- `framework.json.fail-on-unknown-properties`
- `framework.json.max-depth`
- `framework.json.max-payload-size`
- SPI：`JsonCodecCustomizer`、安全允许列表扩展。

## 6. 验收标准与方法

| 场景 | 验收方法 | 通过条件 |
| --- | --- | --- |
| Jackson 默认实现 | 最小应用只引入默认组合 | `JsonCodec` 可用且仅有一个实现 |
| Fastjson2 替代实现 | 使用 Fastjson2 验证应用执行契约套件 | 与 Jackson 的公共语义一致 |
| 实现互斥 | 同时引入两种实现 | 启动失败并给出修复建议 |
| 精度与日期 | 对金额、大整数、日期执行往返测试 | 数据无精度损失，日期符合时区规范 |
| 安全反序列化 | 输入危险类型、超深和超大 JSON | 请求被拒绝，无任意类型实例化 |
| 用户扩展 | 注册受控 Customizer | 扩展生效且不破坏安全基线 |
| 依赖边界 | 检查依赖树 | 不传递引入 Web 或两套实现 |

## 7. 交付物

公共 JSON API、Jackson 默认适配、Fastjson2 适配、共享契约测试、安全配置说明、迁移说明和示例。
