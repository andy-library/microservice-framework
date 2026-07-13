# Microservice Framework 文档

Author: Andy Yang

本目录只保留面向社区的当前产品需求、实现范围、组件契约和验收边界。讨论过程、临时修复记录和未来设想不属于正式需求。

## 正式文档

- [当前实现基线](./Framework-Current-State.md)
- [正式组件目录](./Framework-Component-Catalog.md)
- [Framework 产品需求](./Framework-PRD.md)
- [Parent PRD](./Parent-PRD.md)
- [`starters/`](./starters/)：19 个独立 Starter PRD
- [`../Design/`](../Design/)：Parent 与 19 个 Starter 的技术设计和测试设计

## 文档边界

1. Framework 产品需求定义框架定位、技术栈、能力边界和验收要求。
2. 组件目录定义 Parent、BOM、Starter 和内部复用制品的正式边界。
3. Parent PRD 和 Starter PRD 定义各组件面向开发、运行和验收的详细要求。
4. Design 目录定义与需求匹配的技术设计、测试设计和工程约束。
5. 所有正式源码必须位于 `SourceCode` 或 `Microservice Demo/SourceCode` 下，并能够由 IntelliJ IDEA 直接导入、修改、调试和运行。
6. 文档不得包含访问凭证、组织专属信息、未披露漏洞细节或未经验证的容量承诺。
7. 功能变更必须同步更新对应 PRD、设计、测试和发布说明。

返回 [PRD 总入口](../README.md)。
