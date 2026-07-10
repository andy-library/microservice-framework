# Microservice Framework Database Starter

Author: Andy Yang

数据库 Starter — 提供数据源配置、读写分离、分库分表、事务管理、数据迁移、幂等与 Outbox 支持。

## 核心能力

| 能力 | API 接口 | 说明 |
| --- | --- | --- |
| 数据源定制 | BeanPostProcessor 增强 | 不重复创建 DataSource，而是通过 BeanPostProcessor 将 Framework 配置作为默认值应用到 Spring Boot 自动创建的 HikariDataSource |
| 读写分离 | `ShardingSphereAutoConfiguration` | 读操作路由到从库，写操作路由到主库；写后读一致性保护（`readAfterWriteRoutePrimary`） |
| 分库分表 | `ShardingSphereAutoConfiguration` | ShardingSphere-JDBC 分片路由，按 `@ConditionalOnClass` 自动检测激活 |
| 事务管理 | `TransactionTemplateFacade` | 统一事务模板，支持超时和隔离级别配置 |
| 数据迁移 | `MigrationAutoConfiguration` | Flyway 集成，`@ConditionalOnClass(Flyway)` 自动检测激活 |
| 幂等存储 | `IdempotencyStore` | 数据库幂等去重 |
| Outbox 发布 | `OutboxPublisher` | 事件 Outbox 模式，确保业务操作与事件发布的原子性 |
| 路由提示 | `RoutingHint` | 读写路由强制提示，用于指定操作路由到主库或从库 |

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-database-starter</artifactId>
</dependency>
```

版本由 `microservice-framework-bom` 统一管理，无需指定。

按需额外引入（Starter 通过 `@ConditionalOnClass` 自动检测）：

```xml
<!-- 读写分离 / 分库分表（按需） -->
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc</artifactId>
</dependency>

<!-- 数据迁移（按需） -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### 2. 最小配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: pass
    hikari:
      maximum-pool-size: 10      # Spring Boot 属性优先级最高

framework:
  database:
    data-source:
      pool-size: 10              # Framework 默认值（Spring Boot 属性未设置时生效）
      timeout: 30000             # 连接超时（毫秒）
      slow-sql-threshold: 1000   # 慢 SQL 检测阈值（毫秒）
    rw:
      enabled: true              # 是否启用读写分离
      read-after-write-route-primary: true  # 写后读是否路由到主库
    sharding:
      enabled: false             # 是否启用分库分表
      rules: ""                  # ShardingSphere 分片规则
    transaction:
      default-timeout: 30        # 事务默认超时（秒）
      default-isolation: READ_COMMITTED  # 默认隔离级别
    migration:
      enabled: false             # 是否启用 Flyway 数据迁移
      locations: "classpath:db/migration"  # 迁移脚本位置
```

### 3. 使用示例

```java
@Autowired
private TransactionTemplateFacade txTemplate;

public void createOrder(Order order) {
    txTemplate.execute(status -> {
        orderRepository.save(order);
        outboxPublisher.publish("order-created", order);
        return null;
    });
}
```

## 配置参考

所有配置前缀为 `framework.database`。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `data-source.pool-size` | `10` | HikariCP 连接池大小（作为默认值，Spring Boot 属性优先） |
| `data-source.timeout` | `30000` | 连接超时（毫秒） |
| `data-source.slow-sql-threshold` | `1000` | 慢 SQL 检测阈值（毫秒），0 表示不检测 |
| `rw.enabled` | `true` | 是否启用读写分离 |
| `rw.read-after-write-route-primary` | `true` | 写后读是否路由到主库 |
| `sharding.enabled` | `false` | 是否启用分库分表 |
| `sharding.rules` | `""` | ShardingSphere 分片规则（YAML 格式） |
| `transaction.default-timeout` | `30` | 事务默认超时（秒） |
| `transaction.default-isolation` | `READ_COMMITTED` | 默认隔离级别 |
| `migration.enabled` | `false` | 是否启用 Flyway 数据迁移 |
| `migration.locations` | `classpath:db/migration` | 迁移脚本位置 |

## 自动注册 Bean

Starter 包含四个独立自动配置类，按条件激活：

| 自动配置类 | 激活条件 | 注册 Bean |
| --- | --- | --- |
| `DatabaseDataSourceAutoConfiguration` | HikariDataSource 在 classpath | `HikariDataSourceCustomizer`（BeanPostProcessor） |
| `DatabaseTransactionAutoConfiguration` | DataSource 在容器中 | `TransactionTemplateFacade` |
| `ShardingSphereAutoConfiguration` | ShardingSphere 类在 classpath + `framework.database.sharding.enabled=true` | ShardingSphere DataSource |
| `MigrationAutoConfiguration` | Flyway 在 classpath + `framework.database.migration.enabled=true` | Flyway 配置 |

用户可通过注册自定义同类型 Bean 覆盖默认实现。

## 集成测试策略

| 状态 | 说明 |
| --- | --- |
| 当前覆盖 | 自动配置条件、属性绑定、API 契约（H2 内存数据库 + mock DataSource） |
| 真实中间件 | 暂未包含 |
| 计划方案 | Testcontainers PostgreSQL 验证读写分离路由、事务隔离、迁移脚本执行、ShardingSphere 分片等需要真实数据库行为的场景 |
| 执行方式 | Failsafe + `@Tag("integration")` profile，不在默认 `mvn clean verify` 中 |
| 补齐时间 | 下一迭代 |

当前 `mvn clean verify` 仍为默认准入门禁。真实中间件集成测试将在 Dependencies BOM 补齐对应 Testcontainers 模块后落地。
