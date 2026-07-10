package com.microservice.framework.elasticsearch;

import com.microservice.framework.elasticsearch.ElasticsearchProperties.ConnectionProperties;
import com.microservice.framework.elasticsearch.ElasticsearchProperties.IndexProperties;
import com.microservice.framework.elasticsearch.ElasticsearchProperties.QueryProperties;
import com.microservice.framework.elasticsearch.ElasticsearchProperties.BulkProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ElasticsearchProperties 属性绑定和默认值测试。
 *
 * <p>验证配置属性的默认值和自定义属性绑定行为。
 */
class ElasticsearchPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTest {

        @Test
        @DisplayName("默认 enabled 应为 true")
        void defaultEnabledShouldBeTrue() {
            ElasticsearchProperties props = new ElasticsearchProperties();
            assertThat(props.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认连接配置应有正确的默认值")
        void defaultConnectionPropertiesShouldHaveCorrectDefaults() {
            ConnectionProperties conn = new ConnectionProperties();
            assertThat(conn.getUris()).containsExactly("http://localhost:9200");
            assertThat(conn.getUsername()).isEmpty();
            assertThat(conn.getPassword()).isEmpty();
            assertThat(conn.getConnectTimeout()).isEqualTo(5000);
            assertThat(conn.getSocketTimeout()).isEqualTo(30000);
        }

        @Test
        @DisplayName("默认索引配置应有正确的默认值")
        void defaultIndexPropertiesShouldHaveCorrectDefaults() {
            IndexProperties index = new IndexProperties();
            assertThat(index.isAutoCreate()).isFalse();
            assertThat(index.getRefreshPolicy()).isEqualTo("IMMEDIATE");
        }

        @Test
        @DisplayName("默认查询配置应有正确的默认值")
        void defaultQueryPropertiesShouldHaveCorrectDefaults() {
            QueryProperties query = new QueryProperties();
            assertThat(query.getDefaultSize()).isEqualTo(10);
            assertThat(query.getMaxSize()).isEqualTo(10000);
            assertThat(query.getMaxFromSize()).isEqualTo(10000);
        }

        @Test
        @DisplayName("默认批量操作配置应有正确的默认值")
        void defaultBulkPropertiesShouldHaveCorrectDefaults() {
            BulkProperties bulk = new BulkProperties();
            assertThat(bulk.getBatchSize()).isEqualTo(1000);
            assertThat(bulk.getFlushInterval()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("嵌套属性访问测试")
    class NestedPropertyAccessTest {

        @Test
        @DisplayName("ElasticsearchProperties 应包含所有嵌套属性组")
        void propertiesShouldContainAllNestedGroups() {
            ElasticsearchProperties props = new ElasticsearchProperties();
            assertThat(props.getConnection()).isNotNull();
            assertThat(props.getIndex()).isNotNull();
            assertThat(props.getQuery()).isNotNull();
            assertThat(props.getBulk()).isNotNull();
        }

        @Test
        @DisplayName("嵌套属性应可被替换")
        void nestedPropertiesShouldBeReplaceable() {
            ElasticsearchProperties props = new ElasticsearchProperties();
            ConnectionProperties customConn = new ConnectionProperties();
            customConn.setConnectTimeout(10000);
            props.setConnection(customConn);

            assertThat(props.getConnection().getConnectTimeout()).isEqualTo(10000);
        }
    }

    @Nested
    @DisplayName("自定义值设置测试")
    class CustomValueTest {

        @Test
        @DisplayName("enabled 应可设置为 false")
        void enabledShouldBeSettable() {
            ElasticsearchProperties props = new ElasticsearchProperties();
            props.setEnabled(false);
            assertThat(props.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("连接 URI 应可自定义")
        void connectionUrisShouldBeCustomizable() {
            ConnectionProperties conn = new ConnectionProperties();
            conn.setUris(java.util.List.of("http://es-node1:9200", "http://es-node2:9200"));
            assertThat(conn.getUris()).containsExactly("http://es-node1:9200", "http://es-node2:9200");
        }

        @Test
        @DisplayName("连接认证信息应可自定义")
        void connectionCredentialsShouldBeCustomizable() {
            ConnectionProperties conn = new ConnectionProperties();
            conn.setUsername("admin");
            conn.setPassword("secret");
            assertThat(conn.getUsername()).isEqualTo("admin");
            assertThat(conn.getPassword()).isEqualTo("secret");
        }

        @Test
        @DisplayName("查询分页参数应可自定义")
        void queryPaginationShouldBeCustomizable() {
            QueryProperties query = new QueryProperties();
            query.setDefaultSize(50);
            query.setMaxSize(50000);
            query.setMaxFromSize(20000);
            assertThat(query.getDefaultSize()).isEqualTo(50);
            assertThat(query.getMaxSize()).isEqualTo(50000);
            assertThat(query.getMaxFromSize()).isEqualTo(20000);
        }

        @Test
        @DisplayName("批量操作参数应可自定义")
        void bulkPropertiesShouldBeCustomizable() {
            BulkProperties bulk = new BulkProperties();
            bulk.setBatchSize(500);
            bulk.setFlushInterval(3000);
            assertThat(bulk.getBatchSize()).isEqualTo(500);
            assertThat(bulk.getFlushInterval()).isEqualTo(3000);
        }
    }
}
