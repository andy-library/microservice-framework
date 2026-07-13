# Microservice Framework 公开文档

Author: Andy Yang

本目录公开 Microservice Framework 的产品需求、架构约束、组件契约、技术设计和验收方法。文档描述当前代码基线及其稳定边界，供使用者评估、集成、贡献和审查。

## 阅读路径

1. [Framework 产品需求](./Requirements/Framework-PRD.md)：定位、范围、总体能力和架构约束。
2. [组件目录](./Requirements/Framework-Component-Catalog.md)：Parent、BOM 与 19 个 Starter 的组合边界。
3. [当前实现范围](./Requirements/Framework-Current-State.md)：代码中已经交付的模块和能力边界。
4. [Parent PRD](./Requirements/Parent-PRD.md)：构建、依赖和版本治理契约。
5. [Starter PRD](./Requirements/starters/README.md)：各 Starter 的公共能力与验收标准。
6. [技术设计](./Design/README.md)：工程基线、技术栈、组件设计和测试策略。

## 文档性质

- `Requirements` 定义公开产品契约和验收条件。
- `Design` 说明当前实现采用的技术方案与测试方法。
- 文档中的“必须”表示框架契约，不代表仅凭引入 Starter 即可替代容量规划、安全评审、灾备演练或业务正确性设计。
- 实现、文档和测试应在同一个 Pull Request 中同步变更；存在差异时，以已发布版本的代码、自动化测试和发布说明共同判定。
- 未实现的设想不写入当前基线；演进提案应先通过 Issue 或 ADR 讨论。

## 公开边界

本目录不包含组织专属拓扑、访问凭证、生产数据、内部交付过程、未披露漏洞细节或未经验证的容量承诺。安全问题请按照仓库根目录的 [安全策略](../SECURITY.md) 私下报告。
