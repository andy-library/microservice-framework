package com.microservice.framework.database.autoconfigure;

import com.microservice.framework.database.DatabaseProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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
@ConditionalOnProperty(prefix = "framework.database", name = "sharding.enabled", havingValue = "false", matchIfMissing = true)
public class ShardingSphereAutoConfiguration {

    /**
     * ShardingSphere 配置骨架
     * <p>
     * 当 ShardingSphere-JDBC 在 classpath 上且分库分表未启用时，
     * 提供读写分离的基础配置支持。
     * <p>
     * 具体的 ShardingSphere DataSource Bean 创建由 ShardingSphere-JDBC
     * 自带的 Spring Boot Starter 负责，本配置类仅确保 Framework 属性
     * 与 ShardingSphere 属性的协调一致。
     */
    public ShardingSphereAutoConfiguration(DatabaseProperties databaseProperties) {
        // 当前版本仅提供条件激活检查和属性注入
        // ShardingSphere DataSource 的创建依赖 ShardingSphere-JDBC Spring Boot Starter
        // 后续版本将实现完整的 DataSource 代理逻辑
    }
}
