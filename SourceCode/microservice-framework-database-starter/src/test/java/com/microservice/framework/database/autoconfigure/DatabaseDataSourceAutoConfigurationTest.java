package com.microservice.framework.database.autoconfigure;

import com.microservice.framework.database.DatabaseProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DatabaseDataSourceAutoConfiguration 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证数据源配置的激活条件、
 * HikariCP 默认参数和 Framework 连接池定制行为。
 *
 * @author Andy Yang
 */
class DatabaseDataSourceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                    DatabaseDataSourceAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:testdb",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=");

    @Nested
    @DisplayName("默认配置")
    class DefaultConfiguration {

        @Test
        @DisplayName("默认配置应创建 DataSource Bean")
        void defaultConfigurationShouldCreateDataSource() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("dataSource");
                DataSource dataSource = context.getBean(DataSource.class);
                assertThat(dataSource).isNotNull();
            });
        }

        @Test
        @DisplayName("默认配置应创建 HikariDataSource")
        void defaultConfigurationShouldCreateHikariDataSource() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                DataSource dataSource = context.getBean(DataSource.class);
                assertThat(dataSource).isInstanceOf(HikariDataSource.class);
            });
        }

        @Test
        @DisplayName("默认配置应创建 HikariDataSourceCustomizer Bean")
        void defaultConfigurationShouldCreateCustomizer() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("hikariDataSourceCustomizer");
            });
        }
    }

    @Nested
    @DisplayName("Framework 连接池参数")
    class FrameworkPoolSettings {

        @Test
        @DisplayName("Framework 连接池参数应覆盖 HikariCP 默认值")
        void frameworkPoolSettingsShouldOverrideDefaults() {
            contextRunner.withPropertyValues(
                    "framework.database.data-source.pool-size=20",
                    "framework.database.data-source.timeout=60000")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        DataSource dataSource = context.getBean(DataSource.class);
                        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
                        HikariDataSource hikari = (HikariDataSource) dataSource;
                        assertThat(hikari.getMaximumPoolSize()).isEqualTo(20);
                        assertThat(hikari.getConnectionTimeout()).isEqualTo(60000);
                    });
        }

        @Test
        @DisplayName("Framework 默认值与 HikariCP 默认值相同时不覆盖")
        void frameworkDefaultsShouldNotOverrideWhenSameAsHikariDefaults() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                DataSource dataSource = context.getBean(DataSource.class);
                assertThat(dataSource).isInstanceOf(HikariDataSource.class);
                HikariDataSource hikari = (HikariDataSource) dataSource;
                // HikariCP defaults: maximumPoolSize=10, connectionTimeout=30000
                assertThat(hikari.getMaximumPoolSize()).isEqualTo(10);
                assertThat(hikari.getConnectionTimeout()).isEqualTo(30000);
            });
        }
    }

    @Nested
    @DisplayName("用户自定义覆盖")
    class UserOverride {

        @Test
        @DisplayName("用户通过 Spring Boot 属性配置的连接池参数应优先于 Framework 配置")
        void springBootPropertiesShouldTakePriorityOverFramework() {
            contextRunner.withPropertyValues(
                    "spring.datasource.hikari.maximum-pool-size=5",
                    "framework.database.data-source.pool-size=20")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        DataSource dataSource = context.getBean(DataSource.class);
                        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
                        HikariDataSource hikari = (HikariDataSource) dataSource;
                        // Spring Boot 属性先设置，BeanPostProcessor 发现值不是 HikariCP 默认值(10)，不覆盖
                        assertThat(hikari.getMaximumPoolSize()).isEqualTo(5);
                    });
        }
    }
}
