# 集成测试策略说明

Author: Andy Yang

生成日期：2026-06-16  
适用范围：Framework Starter 全部中间件模块

## 1. 当前状态

当前所有 Starter 的 `mvn clean verify` 均为纯单元测试和自动配置测试，
不执行集成测试（`*IT.java`、`*IntegrationTest.java`、`*E2ETest.java`）。

检查命令：

```bash
find SourceCodes/microservice-framework-*-starter/src/test -type f \
  \( -name '*IT.java' -o -name '*IntegrationTest.java' -o -name '*E2ETest.java' \) \
  -print | sort
```

结果：无输出。

## 2. 需要集成测试的中间件

| Starter | 是否需要 Testcontainers | 说明 |
| --- | --- | --- |
| Redis Starter | 是 | `RedisAutoConfiguration` 创建 `RedisTemplate`、`StringRedisTemplate` 等依赖真实 Redis 服务器的 Bean。单元测试使用 ApplicationContextRunner + FakeRedisConnectionFactory 覆盖自动配置逻辑，但不验证真实 Redis 操作行为 |
| Kafka Starter | 是 | Kafka Producer/Consumer 需要真实 Kafka Broker。当前测试为接口契约测试和自动配置测试，不验证消息收发 |
| Elasticsearch Starter | 是 | `DefaultElasticsearchOperations` 和 `DefaultIndexManager` 委托 `ElasticsearchTemplate`，需要真实 ES 节点验证索引操作 |

## 3. 不需要集成测试的 Starter

| Starter | 原因 |
| --- | --- |
| Web Starter | 纯 Servlet/Spring MVC 逻辑，`MockHttpServletRequest`/`MockHttpServletResponse` 完全覆盖 |
| Logging Starter | Logback TurboFilter/Converter 不依赖外部服务 |
| Observability Starter | Tracing/Metrics 切面不依赖外部服务 |
| Apollo/Nacos Starter | 配中心客户端配置测试可使用 mock server |
| 其他无中间件依赖的 Starter | 纯逻辑组件 |

## 4. 默认 `mvn clean verify` 行为

- **不执行集成测试**
- 集成测试通过 Maven Failsafe Plugin 在 `-Pintegration-test` profile 下执行
- 默认 profile 只运行 Surefire（单元测试）

## 5. 集成测试 Profile

```xml
<profile>
    <id>integration-test</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

使用方式：

```bash
# 仅单元测试（默认）
mvn clean verify

# 单元测试 + 集成测试
mvn clean verify -Pintegration-test
```

## 6. 后续补齐计划

| 优先级 | 任务 | 目标时间 |
| --- | --- | --- |
| P1 | Redis Starter Testcontainers IT | 下个迭代 |
| P1 | Kafka Starter Testcontainers IT | 下个迭代 |
| P2 | Elasticsearch Starter Testcontainers IT | 下个迭代 |
| P2 | Parent 添加 integration-test profile | 下个迭代 |
| P3 | Database Starter IT（需真实数据库） | 未来迭代 |

## 7. Testcontainers 依赖说明

当补齐集成测试时，需要在 Parent 或对应 Starter 的 pom.xml 添加：

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<!-- Redis -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>redis</artifactId>
    <scope>test</scope>
</dependency>
<!-- Kafka -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>
<!-- Elasticsearch -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>elasticsearch</artifactId>
    <scope>test</scope>
</dependency>
```

版本由 `microservice-framework-dependencies` BOM 统一管理。
