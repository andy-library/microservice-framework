# Elasticsearch Starter 技术与测试设计

Author: Andy Yang

对应需求：[Elasticsearch PRD](../../Requirements/starters/elasticsearch-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Elasticsearch 官方 Java API Client、Apache HTTP Client 传输。客户端版本由 Dependencies BOM 显式锁定并与企业集群主版本兼容。

## 2. 技术设计

- 自动配置：Client、IndexGovernance、QueryGovernance、Bulk。
- 公共 API：`IndexDefinition`、`IndexManager`、`SearchQuery`、`SearchResult`、`BulkWriter`。
- 生产索引必须注册 Template/Mapping；动态 Mapping 默认关闭。
- 深分页使用 `search_after` + PIT 受控封装；普通分页设置硬上限。
- Bulk 按条返回结果，重试仅作用于可重试条目；Alias 切换原子执行。
- V1 公共 API 可保持领域中立，但不实现 OpenSearch 适配层。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 自动配置 | TLS/认证、超时、Bulk、非法索引配置 |
| 集成 | Testcontainers Elasticsearch；模板、查询、PIT、Bulk、Alias |
| 故障 | 节点中断、超时、Bulk 部分失败、恢复 |
| 迁移 | 新索引重建、Alias 切换、失败回滚 |
| 治理 | 动态字段、深分页、超大结果和慢查询阻断 |
| 性能 | 查询与 Bulk 吞吐、批次和并发基准 |

通过条件：部分失败精确报告；查询边界生效；Alias 迁移可回滚。
