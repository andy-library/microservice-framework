package com.microservice.framework.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DatabaseProperties 单元测试
 * <p>
 * 验证配置属性绑定、默认值和嵌套配置的正确性。
 *
 * @author Andy Yang
 */
class DatabasePropertiesTest {

    @Nested
    @DisplayName("默认值验证")
    class DefaultValues {

        @Test
        @DisplayName("新建 DatabaseProperties 应具有所有默认值")
        void shouldHaveAllDefaults() {
            DatabaseProperties props = new DatabaseProperties();

            // DataSource 默认值
            assertThat(props.getDataSource().getPoolSize()).isEqualTo(10);
            assertThat(props.getDataSource().getTimeout()).isEqualTo(30000L);
            assertThat(props.getDataSource().getSlowSqlThreshold()).isEqualTo(1000L);

            // Sharding 默认值
            assertThat(props.getSharding().isEnabled()).isFalse();
            assertThat(props.getSharding().getRules()).isEmpty();

            // RW 默认值
            assertThat(props.getRw().isEnabled()).isTrue();
            assertThat(props.getRw().isReadAfterWriteRoutePrimary()).isTrue();

            // Transaction 默认值
            assertThat(props.getTransaction().getDefaultTimeout()).isEqualTo(30);
            assertThat(props.getTransaction().getDefaultIsolation()).isEqualTo("READ_COMMITTED");

            // Migration 默认值
            assertThat(props.getMigration().isEnabled()).isFalse();
            assertThat(props.getMigration().getLocations()).isEqualTo("classpath:db/migration");
        }
    }

    @Nested
    @DisplayName("属性绑定")
    class PropertyBinding {

        @Test
        @DisplayName("应能修改 DataSource 属性")
        void shouldModifyDataSourceProperties() {
            DatabaseProperties props = new DatabaseProperties();
            props.getDataSource().setPoolSize(20);
            props.getDataSource().setTimeout(60000L);
            props.getDataSource().setSlowSqlThreshold(2000L);

            assertThat(props.getDataSource().getPoolSize()).isEqualTo(20);
            assertThat(props.getDataSource().getTimeout()).isEqualTo(60000L);
            assertThat(props.getDataSource().getSlowSqlThreshold()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("应能修改 Sharding 属性")
        void shouldModifyShardingProperties() {
            DatabaseProperties props = new DatabaseProperties();
            props.getSharding().setEnabled(true);
            props.getSharding().setRules("tables: { t_order: { actualDataNodes: ds0.t_order_0..1 } }");

            assertThat(props.getSharding().isEnabled()).isTrue();
            assertThat(props.getSharding().getRules()).isNotEmpty();
        }

        @Test
        @DisplayName("应能修改 RW 属性")
        void shouldModifyRwProperties() {
            DatabaseProperties props = new DatabaseProperties();
            props.getRw().setEnabled(false);
            props.getRw().setReadAfterWriteRoutePrimary(false);

            assertThat(props.getRw().isEnabled()).isFalse();
            assertThat(props.getRw().isReadAfterWriteRoutePrimary()).isFalse();
        }

        @Test
        @DisplayName("应能修改 Transaction 属性")
        void shouldModifyTransactionProperties() {
            DatabaseProperties props = new DatabaseProperties();
            props.getTransaction().setDefaultTimeout(60);
            props.getTransaction().setDefaultIsolation("SERIALIZABLE");

            assertThat(props.getTransaction().getDefaultTimeout()).isEqualTo(60);
            assertThat(props.getTransaction().getDefaultIsolation()).isEqualTo("SERIALIZABLE");
        }

        @Test
        @DisplayName("应能修改 Migration 属性")
        void shouldModifyMigrationProperties() {
            DatabaseProperties props = new DatabaseProperties();
            props.getMigration().setEnabled(true);
            props.getMigration().setLocations("classpath:db/migration,filesystem:/opt/migrations");

            assertThat(props.getMigration().isEnabled()).isTrue();
            assertThat(props.getMigration().getLocations()).isEqualTo("classpath:db/migration,filesystem:/opt/migrations");
        }
    }

    @Nested
    @DisplayName("嵌套配置独立性")
    class NestedIndependence {

        @Test
        @DisplayName("修改一个嵌套属性不应影响其他嵌套属性")
        void modifyingOneNestedShouldNotAffectOthers() {
            DatabaseProperties props = new DatabaseProperties();
            props.getDataSource().setPoolSize(50);
            props.getSharding().setEnabled(true);

            // 其他属性应保持默认值
            assertThat(props.getRw().isEnabled()).isTrue();
            assertThat(props.getTransaction().getDefaultTimeout()).isEqualTo(30);
            assertThat(props.getMigration().isEnabled()).isFalse();
        }
    }

    @Test
    @DisplayName("配置前缀应为 framework.database")
    void configurationPrefixShouldBeCorrect() {
        ConfigurationProperties annotation = DatabaseProperties.class
                .getAnnotation(ConfigurationProperties.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("framework.database");
    }
}
