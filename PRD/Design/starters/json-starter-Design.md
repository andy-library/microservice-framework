# JSON Starter 技术与测试设计

Author: Andy Yang

对应需求：[JSON PRD](../../PRD/starters/json-starter-PRD.md)

## 1. 技术栈

Common Starter、Spring Boot JSON/Jackson 依赖、Fastjson2 `2.0.54`（已由当前 Dependencies BOM 管理）、Jakarta Validation。

## 2. 技术设计

- 公共 API：`JsonCodec`、`JsonTypeReference<T>`、`JsonCodecException`、`JsonCodecCustomizer`。
- 默认 `JacksonJsonCodec` 使用 Boot 管理的 `ObjectMapper`；替代实现 `Fastjson2JsonCodec`。
- `JsonAutoConfiguration` 选择 provider；`JacksonJsonAutoConfiguration` 与 `Fastjson2JsonAutoConfiguration` 使用互斥条件。
- 同时出现两个实现、配置与类路径不一致、没有实现时通过 `FailureAnalyzer` 阻断启动。
- 公共契约统一日期、金额、枚举、空值、未知字段、泛型和异常语义。
- 禁止危险自动类型；限制最大深度和载荷；业务扩展仅通过 Customizer。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 共享契约 | 两个 Provider 对全部公共类型执行相同往返用例 |
| 安全 | 多态攻击、超深、超大、未知类型和异常信息脱敏 |
| 自动配置 | Jackson 默认、Fastjson2 显式选择、双实现冲突、用户覆盖 |
| 集成 | Web/Kafka/Redis 适配验证使用同一 `JsonCodec` 语义 |
| 架构 | 不传递 Web；Fastjson2 v1 被 Parent 阻断 |
| 性能 | 常见 DTO、小对象与大集合 JMH，对比基线并记录差异 |

通过条件：两个实现契约一致；运行期不可切换；危险反序列化全部失败关闭。
