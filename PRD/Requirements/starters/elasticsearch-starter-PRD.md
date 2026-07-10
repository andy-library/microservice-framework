# Elasticsearch Starter PRD

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 制品 | `microservice-framework-elasticsearch-starter` |
| 引入策略 | 按需显式引入 |
| V1 范围 | 仅支持 Elasticsearch，不抽象 OpenSearch |

## 1. 定位与边界

Elasticsearch 为应用提供统一索引、查询、批量处理和运行治理能力。它适用于搜索和分析，不作为业务唯一事实数据源。

组件不提供 OpenSearch 兼容层，不隐藏索引建模责任，也不允许在请求链路执行无界查询。

## 2. 技术栈与依赖

- Elasticsearch 官方 Java API Client。
- 依赖 Common、JSON、Logging；Observability 可选。

## 3. 核心能力

1. 客户端、认证、TLS、连接、超时和重试治理。
2. Index Template、Mapping、Alias、版本迁移和回滚。
3. 统一查询、分页、游标、批量写入和失败明细。
4. 慢查询、集群状态、批量失败和积压监控。
5. 索引重建、别名切换和数据修复流程。

## 4. 实现要求

- 禁止自动动态 Mapping 造成不可控字段膨胀；生产索引必须有模板。
- 深分页使用受治理游标方案；查询必须有超时、结果上限和字段限制。
- Bulk 操作必须限制批次和并发，逐项识别失败，不得把部分失败报告为成功。
- 索引版本迁移采用新索引构建与 Alias 原子切换，保留回滚路径。
- 集群不可用时失败必须可诊断；核心事实数据仍由业务数据库保障。

## 5. 配置与扩展

- `framework.elasticsearch.uris`
- `framework.elasticsearch.security.*`
- `framework.elasticsearch.timeout.*`
- `framework.elasticsearch.bulk.*`
- `framework.elasticsearch.query.*`
- SPI：索引定义、查询治理和批量失败处理。

## 6. 验收标准与方法

| 场景 | 验收方法 | 通过条件 |
| --- | --- | --- |
| 安全连接 | 使用 TLS 与认证启动验证应用 | 连接成功，凭据不泄漏 |
| Mapping 治理 | 写入未知/非法字段 | 按模板规则拒绝或受控处理 |
| 深分页 | 请求超出普通分页边界 | 普通深分页被阻断，游标方案可用 |
| Bulk 部分失败 | 构造混合成功失败批次 | 返回逐项结果，可重试项可识别 |
| Alias 迁移 | 重建索引并切换别名 | 切换原子，失败可回滚 |
| 集群中断 | 断开集群并发起请求 | 超时有界、错误清晰、业务不伪成功 |

## 7. 交付物

自动配置、索引与查询规范、迁移工具契约、压力基准、故障演练和验证应用。
