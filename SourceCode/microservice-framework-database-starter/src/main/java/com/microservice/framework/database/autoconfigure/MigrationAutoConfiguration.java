package com.microservice.framework.database.autoconfigure;

import com.microservice.framework.database.DatabaseProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 数据迁移自动配置
 * <p>
 * 当 {@code Flyway} 在 classpath 上且 {@code framework.database.migration.enabled=true} 时，
 * 配置 Flyway 数据迁移并应用 Framework 属性。
 * <p>
 * 激活条件：
 * <ul>
 *   <li>{@code org.flywaydb.core.Flyway} 在 classpath 上</li>
 *   <li>{@code framework.database.migration.enabled=true}（默认 false，需显式开启）</li>
 * </ul>
 * <p>
 * 用户可通过 Spring Boot 标准 {@code spring.flyway.*} 属性覆盖迁移配置，
 * 也可通过注册自定义 {@code Flyway} Bean 完全替换默认迁移实例。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(DatabaseProperties.class)
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "framework.database.migration", name = "enabled", havingValue = "true")
public class MigrationAutoConfiguration {

    /**
     * Flyway 配置
     * <p>
     * 在 Flyway 可用且迁移启用时，创建 Flyway Bean 并应用 Framework 迁移路径配置。
     */
    @Configuration(proxyBeanMethods = false)
    static class FlywayConfiguration {

        /**
         * 创建 Framework 配置的 Flyway 实例
         * <p>
         * 使用 {@code framework.database.migration.locations} 作为迁移脚本路径，
         * 确保 Framework 和 Spring Boot 的 Flyway 配置协调一致。
         * <p>
         * 如果用户已通过 {@code spring.flyway.*} 或自定义 Bean 配置 Flyway，
         * Spring Boot 的 FlywayAutoConfiguration 将优先处理。
         *
         * @param dataSource         数据源
         * @param databaseProperties Framework 数据库属性
         * @return 配置完成的 Flyway 实例
         */
        @Bean
        public Flyway frameworkFlyway(DataSource dataSource,
                                       DatabaseProperties databaseProperties) {
            DatabaseProperties.MigrationProperties migProps = databaseProperties.getMigration();

            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(migProps.getLocations())
                    .load();

            return flyway;
        }
    }
}
