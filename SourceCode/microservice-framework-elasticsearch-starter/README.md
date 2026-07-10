# Microservice Framework Elasticsearch Starter

Author: Andy Yang

Elasticsearch Starter — 提供索引管理、搜索查询、批量操作和运行治理能力，基于 Spring Data Elasticsearch。

## 核心能力

| 能力 | API 接口 | 说明 |
| --- | --- | --- |
| 索引管理 | `IndexManager` | 索引创建、删除、映射更新、存在检测和刷新 |
| 搜索查询 | `SearchQueryBuilder` + `ElasticsearchOperations` | Builder 模式构建查询（match/term/range/bool），支持分页和排序 |
| 批量操作 | `ElasticsearchOperations.bulkIndex` / `bulkDelete` | 批量索引和批量删除，返回成功/失败明细 |
| 文档 CRUD | `ElasticsearchOperations` | 单文档索引、获取、删除和计数 |

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-elasticsearch-starter</artifactId>
</dependency>
```

版本由 `microservice-framework-bom` 统一管理，无需指定。

### 2. 最小配置

```yaml
framework:
  elasticsearch:
    enabled: true                          # 是否启用 Starter
    connection:
      uris:
        - http://localhost:9200             # ES 集群节点地址
      username: ""                          # 认证用户名
      password: ""                          # 认证密码
      connect-timeout: 5000                 # 连接超时（毫秒）
      socket-timeout: 30000                 # Socket 超时（毫秒）
    index:
      auto-create: false                    # 是否自动创建索引
      refresh-policy: IMMEDIATE             # 索引刷新策略
    query:
      default-size: 10                      # 查询默认返回条数
      max-size: 10000                       # 查询最大返回条数
    bulk:
      batch-size: 1000                      # 批量操作每批大小
      flush-interval: 5000                  # 批量刷新间隔（毫秒）
```

### 3. 使用示例

```java
@Autowired
private ElasticsearchOperations esOps;

public List<Product> searchProducts(String keyword) {
    SearchQueryBuilder builder = SearchQueryBuilder.create()
        .match("name", keyword)
        .range("price", 100, 500)
        .sort("price", SearchQueryBuilder.SortDirection.ASC);
    return esOps.search("products", builder, Product.class);
}
```

## 配置参考

所有配置前缀为 `framework.elasticsearch`。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用 Elasticsearch Starter |
| `connection.uris` | `["http://localhost:9200"]` | ES 集群节点地址列表 |
| `connection.username` | `""` | 认证用户名 |
| `connection.password` | `""` | 认证密码 |
| `connection.connect-timeout` | `5000` | 连接超时（毫秒） |
| `connection.socket-timeout` | `30000` | Socket 超时（毫秒） |
| `index.auto-create` | `false` | 是否自动创建索引 |
| `index.refresh-policy` | `IMMEDIATE` | 索引刷新策略 |
| `query.default-size` | `10` | 查询默认返回条数 |
| `query.max-size` | `10000` | 查询最大返回条数 |
| `bulk.batch-size` | `1000` | 批量操作每批大小 |
| `bulk.flush-interval` | `5000` | 批量刷新间隔（毫秒） |

## 自动注册 Bean

当 Spring Data Elasticsearch 在 classpath 上且 `framework.elasticsearch.enabled=true` 时，自动注册：

| Bean 名称 | 类型 | 条件 |
| --- | --- | --- |
| `elasticsearchOperations` | `ElasticsearchOperations` | `@ConditionalOnMissingBean` |
| `indexManager` | `IndexManager` | `@ConditionalOnMissingBean` |

用户可通过注册自定义同类型 Bean 覆盖默认实现。

## 集成测试策略

| 状态 | 说明 |
| --- | --- |
| 当前覆盖 | 自动配置条件、属性绑定、API 契约（mock ElasticsearchTemplate） |
| 真实中间件 | 暂未包含 |
| 计划方案 | Testcontainers Elasticsearch 模块提供真实 ES 集群，验证索引创建/映射/刷新、搜索查询 DSL、批量操作一致性等需要真实 ES 行为的场景 |
| 执行方式 | Failsafe + `@Tag("integration")` profile，不在默认 `mvn clean verify` 中 |
| 补齐时间 | 下一迭代 |

当前 `mvn clean verify` 仍为默认准入门禁。真实中间件集成测试将在 Dependencies BOM 补齐对应 Testcontainers 模块后落地。
