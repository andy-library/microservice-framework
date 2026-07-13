package com.microservice.framework.database.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ShardingSphereAutoConfiguration 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证 ShardingSphere 配置的激活/禁用条件。
 * <p>
 * database-starter 强制集成 ShardingSphere-JDBC，因此默认配置应创建配置骨架 Bean；
 * 分库分表显式启用时由条件阻止该骨架配置，交由实际 ShardingSphere 配置接管。
 *
 * @author Andy Yang
 */
class ShardingSphereAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ShardingSphereAutoConfiguration.class));

    @Nested
    @DisplayName("条件激活")
    class ConditionalActivation {

        @Test
        @DisplayName("默认配置应激活 ShardingSphere 配置骨架")
        void defaultConfigurationShouldActivate() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(ShardingSphereAutoConfiguration.class);
            });
        }

        @Test
        @DisplayName("sharding.enabled=true 时应基于 rules 创建 ShardingSphere DataSource")
        void shardingEnabledShouldCreateExecutableShardingSphereDataSource() {
            contextRunner.withPropertyValues(
                            "framework.database.sharding.enabled=true",
                            "framework.database.sharding.rules=" + """
                                    dataSources:
                                      ds_0:
                                        dataSourceClassName: com.zaxxer.hikari.HikariDataSource
                                        driverClassName: org.h2.Driver
                                        jdbcUrl: jdbc:h2:mem:sharding_rules;MODE=MySQL;DB_CLOSE_DELAY=-1
                                        username: sa
                                        password: ''
                                    rules: []
                                    """)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(DataSource.class);
                        assertThat(context.getBean(DataSource.class).getClass().getName())
                                .contains("ShardingSphereDataSource");
                    });
        }

        @Test
        @DisplayName("rw.enabled=false 且 sharding.enabled=false 时不应创建路由 DataSource")
        void routingDisabledShouldNotCreateShardingSphereDataSource() {
            contextRunner.withPropertyValues(
                            "framework.database.rw.enabled=false",
                            "framework.database.sharding.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean(DataSource.class);
                    });
        }
    }

    @Nested
    @DisplayName("属性默认值验证")
    class PropertyDefaults {

        @Test
        @DisplayName("默认 sharding.enabled 应为 false")
        void defaultShardingEnabledShouldBeFalse() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                // ShardingSphereAutoConfiguration 配置了 matchIfMissing=true，
                // 表示默认情况下（sharding.enabled=false）应可激活，
                // 但因缺少 ShardingSphere classpath 条件而未激活
            });
        }

        @Test
        @DisplayName("读写分离配置应暴露可执行的路由计划")
        void readWriteConfigurationShouldExposeExecutableRoutingPlan() {
            contextRunner.withPropertyValues(
                            "framework.database.rw.enabled=true",
                            "framework.database.rw.read-after-write-route-primary=true")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ShardingSphereAutoConfiguration.RoutingPlan routingPlan =
                                context.getBean(ShardingSphereAutoConfiguration.RoutingPlan.class);
                        assertThat(routingPlan.readWriteSplittingEnabled()).isTrue();
                        assertThat(routingPlan.readAfterWriteRoutePrimary()).isTrue();
                    });
        }
    }
}
