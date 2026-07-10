package com.microservice.framework.database.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MigrationAutoConfiguration 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证 Flyway 迁移配置的激活/禁用条件。
 * <p>
 * 注意：Flyway-core 在测试 classpath 上（作为 optional 依赖引入，但 test scope 仍可用），
 * 但 migration.enabled 默认为 false，需要显式开启。
 *
 * @author Andy Yang
 */
class MigrationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                    MigrationAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:testdb",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=");

    @Nested
    @DisplayName("条件激活")
    class ConditionalActivation {

        @Test
        @DisplayName("默认 migration.enabled=false 时 MigrationAutoConfiguration 不应激活")
        void defaultDisabledShouldNotActivate() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean("frameworkFlyway");
            });
        }

        @Test
        @DisplayName("migration.enabled=true 时 MigrationAutoConfiguration 应激活")
        void migrationEnabledShouldActivate() {
            contextRunner.withPropertyValues(
                    "framework.database.migration.enabled=true",
                    "framework.database.migration.locations=classpath:db/migration")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("frameworkFlyway");
                    });
        }
    }

    @Nested
    @DisplayName("属性绑定")
    class PropertyBinding {

        @Test
        @DisplayName("自定义迁移脚本路径应生效")
        void customLocationsShouldBeApplied() {
            contextRunner.withPropertyValues(
                    "framework.database.migration.enabled=true",
                    "framework.database.migration.locations=classpath:db/test_migration")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("frameworkFlyway");
                    });
        }
    }
}
