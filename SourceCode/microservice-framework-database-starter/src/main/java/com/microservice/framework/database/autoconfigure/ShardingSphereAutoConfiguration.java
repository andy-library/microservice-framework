package com.microservice.framework.database.autoconfigure;

import com.microservice.framework.database.DatabaseProperties;
import com.microservice.framework.database.api.ReadWriteRoutingContext;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * ShardingSphere 自动配置
 * <p>
 * 当 {@code ShardingSphere-JDBC} 在 classpath 上时，根据配置决定启用读写分离或分库分表。
 * <p>
 * 激活条件：
 * <ul>
 *   <li>{@code org.apache.shardingsphere.shardingjdbc.spring.boot.ShardingSphereAutoConfiguration} 在 classpath 上</li>
 *   <li>{@code framework.database.rw.enabled=true}（默认）或 {@code framework.database.sharding.enabled=true}</li>
 * </ul>
 * <p>
 * 分库分表模式（{@code sharding.enabled=true}）：
 * <ul>
 *   <li>加载 {@code framework.database.sharding.rules} 中定义的分片规则</li>
 *   <li>读写分离配置在分片规则中定义</li>
 * </ul>
 * <p>
 * 读写分离模式（{@code sharding.enabled=false, rw.enabled=true}）：
 * <ul>
 *   <li>仅配置主从路由，不涉及分库分表</li>
 *   <li>{@code rw.read-after-write-route-primary=true} 时写后读路由到主库</li>
 * </ul>
 * <p>
 * 当前仅提供配置骨架和条件判断逻辑，ShardingSphere DataSource 的具体创建
 * 依赖 ShardingSphere-JDBC 5.x 的 Spring Boot Starter。
 * 后续版本将实现完整的 ShardingSphere DataSource 代理逻辑。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(DatabaseProperties.class)
@ConditionalOnClass(name = "org.apache.shardingsphere.driver.ShardingSphereDriver")
public class ShardingSphereAutoConfiguration {

    /**
     * ShardingSphere routing plan exposed to application code and tests.
     */
    @Bean
    @ConditionalOnMissingBean
    RoutingPlan routingPlan(DatabaseProperties databaseProperties) {
        return new RoutingPlan(
                databaseProperties.getRw().isEnabled(),
                databaseProperties.getRw().isReadAfterWriteRoutePrimary(),
                databaseProperties.getSharding().isEnabled());
    }

    @Bean
    @ConditionalOnMissingBean
    ReadWriteRoutingContext readWriteRoutingContext(DatabaseProperties databaseProperties) {
        return new ReadWriteRoutingContext(databaseProperties.getRw().isReadAfterWriteRoutePrimary());
    }

    /**
     * Creates a real ShardingSphere DataSource from framework YAML rules when
     * sharding is enabled.
     *
     * @param databaseProperties framework database properties
     * @return executable ShardingSphere DataSource
     * @throws Exception when rules are missing or invalid
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    @ConditionalOnProperty(prefix = "framework.database.sharding", name = "enabled", havingValue = "true")
    DataSource shardingSphereDataSource(DatabaseProperties databaseProperties) throws Exception {
        String rules = databaseProperties.getSharding().getRules();
        if (rules == null || rules.isBlank()) {
            throw new IllegalArgumentException("framework.database.sharding.rules must not be blank when sharding is enabled");
        }
        return YamlShardingSphereDataSourceFactory.createDataSource(rules.getBytes(StandardCharsets.UTF_8));
    }

    public record RoutingPlan(boolean readWriteSplittingEnabled,
                              boolean readAfterWriteRoutePrimary,
                              boolean shardingEnabled) {
    }
}
