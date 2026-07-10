package com.microservice.framework.database.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database production safety tests.
 */
class DatabaseProductionSafetyAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DatabaseProductionSafetyAutoConfiguration.class));

    @Nested
    @DisplayName("生产准入校验")
    class ProductionSafety {

        @Test
        @DisplayName("生产环境应拒绝过长的数据源连接等待超时")
        void prodShouldRejectExcessiveDatasourceConnectionTimeout() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.database.data-source.timeout=30000")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.database.data-source.timeout must not exceed 5000ms in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境应拒绝关闭慢 SQL 检测")
        void prodShouldRejectDisabledSlowSqlDetection() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.database.data-source.timeout=5000",
                            "framework.database.data-source.slow-sql-threshold=0")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.database.data-source.slow-sql-threshold must be enabled in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境应拒绝过长的默认事务超时")
        void prodShouldRejectExcessiveDefaultTransactionTimeout() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.database.data-source.timeout=5000",
                            "framework.database.transaction.default-timeout=120")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.database.transaction.default-timeout must not exceed 60s in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境启用读写分离时不允许关闭写后读主")
        void prodShouldRejectDisabledReadAfterWriteRoutePrimary() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.database.data-source.timeout=5000",
                            "framework.database.rw.enabled=true",
                            "framework.database.rw.read-after-write-route-primary=false")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.database.rw.read-after-write-route-primary cannot be disabled in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境不允许 Outbox 自动建表")
        void prodShouldRejectOutboxAutoCreateTable() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.database.data-source.timeout=5000",
                            "framework.database.outbox.enabled=true",
                            "framework.database.outbox.auto-create-table=true")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.database.outbox.auto-create-table cannot be enabled in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境应限制 Outbox 单批拉取数量")
        void prodShouldRejectExcessiveOutboxFetchSize() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.database.data-source.timeout=5000",
                            "framework.database.outbox.enabled=true",
                            "framework.database.outbox.max-fetch-size=5000")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.database.outbox.max-fetch-size must not exceed 1000 in prod profile");
                    });
        }

        @Test
        @DisplayName("生产推荐配置应通过准入校验")
        void prodShouldAcceptRecommendedBoundaries() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.database.data-source.timeout=5000",
                            "framework.database.data-source.slow-sql-threshold=1000",
                            "framework.database.transaction.default-timeout=30",
                            "framework.database.rw.enabled=true",
                            "framework.database.rw.read-after-write-route-primary=true",
                            "framework.database.outbox.enabled=true",
                            "framework.database.outbox.auto-create-table=false",
                            "framework.database.outbox.max-fetch-size=500")
                    .run(context -> assertThat(context).hasNotFailed());
        }
    }
}
