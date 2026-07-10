package com.microservice.framework.elasticsearch.autoconfigure;

import com.microservice.framework.elasticsearch.ElasticsearchProperties;
import com.microservice.framework.elasticsearch.api.ElasticsearchOperations;
import com.microservice.framework.elasticsearch.api.IndexManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ElasticsearchAutoConfiguration 自动配置测试。
 *
 * <p>使用 ApplicationContextRunner 验证自动配置的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 */
class ElasticsearchAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

    @Nested
    @DisplayName("默认配置激活测试")
    class DefaultActivationTest {

        @Test
        @DisplayName("默认配置应激活 ElasticsearchOperations 和 IndexManager Bean")
        void defaultConfigurationShouldActivateAll() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("elasticsearchOperations");
                assertThat(context).hasBean("indexManager");
            });
        }

        @Test
        @DisplayName("默认配置应注册正确类型的 Bean")
        void defaultConfigurationShouldRegisterCorrectTypes() {
            contextRunner.run(context -> {
                assertThat(context.getBean(ElasticsearchOperations.class))
                        .isInstanceOf(ElasticsearchAutoConfiguration.DefaultElasticsearchOperations.class);
                assertThat(context.getBean(IndexManager.class))
                        .isInstanceOf(ElasticsearchAutoConfiguration.DefaultIndexManager.class);
            });
        }
    }

    @Nested
    @DisplayName("禁用 Elasticsearch Starter 测试")
    class DisablingTest {

        @Test
        @DisplayName("framework.elasticsearch.enabled=false 时不应注册任何 ES Bean")
        void disablingElasticsearchStarterShouldRemoveAllBeans() {
            contextRunner.withPropertyValues("framework.elasticsearch.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("elasticsearchOperations");
                        assertThat(context).doesNotHaveBean("indexManager");
                        assertThat(context).doesNotHaveBean(ElasticsearchOperations.class);
                        assertThat(context).doesNotHaveBean(IndexManager.class);
                    });
        }
    }

    @Nested
    @DisplayName("属性绑定测试")
    class PropertyBindingTest {

        @Test
        @DisplayName("自定义连接超时应正确绑定")
        void customConnectionTimeoutShouldBeBound() {
            contextRunner.withPropertyValues(
                    "framework.elasticsearch.connection.connectTimeout=10000",
                    "framework.elasticsearch.connection.socketTimeout=60000")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ElasticsearchProperties props = context.getBean(ElasticsearchProperties.class);
                        assertThat(props.getConnection().getConnectTimeout()).isEqualTo(10000);
                        assertThat(props.getConnection().getSocketTimeout()).isEqualTo(60000);
                    });
        }

        @Test
        @DisplayName("自定义索引配置应正确绑定")
        void customIndexPropertiesShouldBeBound() {
            contextRunner.withPropertyValues(
                    "framework.elasticsearch.index.autoCreate=true",
                    "framework.elasticsearch.index.refreshPolicy=WAIT_UNTIL")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ElasticsearchProperties props = context.getBean(ElasticsearchProperties.class);
                        assertThat(props.getIndex().isAutoCreate()).isTrue();
                        assertThat(props.getIndex().getRefreshPolicy()).isEqualTo("WAIT_UNTIL");
                    });
        }

        @Test
        @DisplayName("自定义查询分页参数应正确绑定")
        void customQueryPropertiesShouldBeBound() {
            contextRunner.withPropertyValues(
                    "framework.elasticsearch.query.defaultSize=50",
                    "framework.elasticsearch.query.maxSize=5000")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ElasticsearchProperties props = context.getBean(ElasticsearchProperties.class);
                        assertThat(props.getQuery().getDefaultSize()).isEqualTo(50);
                        assertThat(props.getQuery().getMaxSize()).isEqualTo(5000);
                    });
        }

        @Test
        @DisplayName("自定义批量操作参数应正确绑定")
        void customBulkPropertiesShouldBeBound() {
            contextRunner.withPropertyValues(
                    "framework.elasticsearch.bulk.batchSize=500",
                    "framework.elasticsearch.bulk.flushInterval=3000")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ElasticsearchProperties props = context.getBean(ElasticsearchProperties.class);
                        assertThat(props.getBulk().getBatchSize()).isEqualTo(500);
                        assertThat(props.getBulk().getFlushInterval()).isEqualTo(3000);
                    });
        }
    }

    @Nested
    @DisplayName("生产准入测试")
    class ProductionSafetyTest {

        @Test
        @DisplayName("生产环境不允许使用本地 Elasticsearch 地址")
        void prodShouldRejectLocalhostUris() {
            contextRunner.withPropertyValues(
                    "spring.profiles.active=prod",
                    "framework.elasticsearch.connection.uris[0]=http://127.0.0.1:9200")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("local Elasticsearch uri is not allowed in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境不允许自动创建索引")
        void prodShouldRejectAutoCreateIndex() {
            contextRunner.withPropertyValues(
                    "spring.profiles.active=prod",
                    "framework.elasticsearch.connection.uris[0]=http://es-prod:9200",
                    "framework.elasticsearch.index.auto-create=true")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.elasticsearch.index.auto-create cannot be enabled in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境不允许使用 IMMEDIATE 刷新策略")
        void prodShouldRejectImmediateRefreshPolicy() {
            contextRunner.withPropertyValues(
                    "spring.profiles.active=prod",
                    "framework.elasticsearch.connection.uris[0]=http://es-prod:9200",
                    "framework.elasticsearch.index.refresh-policy=IMMEDIATE")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("IMMEDIATE refresh policy is not allowed in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境不允许超过默认安全查询窗口")
        void prodShouldRejectOverSizedQueryWindow() {
            contextRunner.withPropertyValues(
                    "spring.profiles.active=prod",
                    "framework.elasticsearch.connection.uris[0]=http://es-prod:9200",
                    "framework.elasticsearch.index.refresh-policy=WAIT_UNTIL",
                    "framework.elasticsearch.query.max-from-size=20000")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.elasticsearch.query.max-from-size must not exceed 10000 in prod profile");
                    });
        }
    }

    @Nested
    @DisplayName("用户自定义 Bean 覆盖测试")
    class UserOverrideTest {

        @Test
        @DisplayName("用户提供的 ElasticsearchOperations 应覆盖默认 Bean")
        void userProvidedElasticsearchOperationsShouldOverrideDefault() {
            ElasticsearchOperations customOps = new ElasticsearchOperations() {
                @Override public String index(String indexName, Object document, String id) { return id; }
                @Override public <T> java.util.Optional<T> get(String indexName, String id, Class<T> clazz) { return java.util.Optional.empty(); }
                @Override public String delete(String indexName, String id) { return id; }
                @Override public <T> java.util.List<T> search(String indexName, com.microservice.framework.elasticsearch.api.SearchQueryBuilder builder, Class<T> clazz) { return java.util.List.of(); }
                @Override public <T> java.util.List<T> search(String indexName, com.microservice.framework.elasticsearch.api.SearchQueryBuilder builder, org.springframework.data.domain.Pageable pageable, Class<T> clazz) { return java.util.List.of(); }
                @Override public ElasticsearchOperations.BulkResult bulkIndex(String indexName, java.util.List<?> documents) { return new ElasticsearchOperations.BulkResult(java.util.List.of(), java.util.List.of(), java.util.Map.of()); }
                @Override public ElasticsearchOperations.BulkResult bulkDelete(String indexName, java.util.List<String> ids) { return new ElasticsearchOperations.BulkResult(java.util.List.of(), java.util.List.of(), java.util.Map.of()); }
                @Override public long count(String indexName, com.microservice.framework.elasticsearch.api.SearchQueryBuilder builder) { return 0L; }
            };

            contextRunner.withBean("customElasticsearchOperations", ElasticsearchOperations.class,
                    () -> customOps)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customElasticsearchOperations");
                        assertThat(context).doesNotHaveBean("elasticsearchOperations");
                        assertThat(context.getBean(ElasticsearchOperations.class)).isSameAs(customOps);
                    });
        }

        @Test
        @DisplayName("用户提供的 IndexManager 应覆盖默认 Bean")
        void userProvidedIndexManagerShouldOverrideDefault() {
            IndexManager customManager = new IndexManager() {
                @Override public boolean createIndex(String indexName) { return true; }
                @Override public boolean createIndex(String indexName, String mapping) { return true; }
                @Override public boolean deleteIndex(String indexName) { return true; }
                @Override public boolean indexExists(String indexName) { return true; }
                @Override public void refreshIndex(String indexName) { }
                @Override public void putMapping(String indexName, String mapping) { }
                @Override public boolean aliasExists(String aliasName) { return true; }
                @Override public boolean createAlias(String indexName, String aliasName) { return true; }
                @Override public boolean switchAlias(String aliasName, String fromIndex, String toIndex) { return true; }
            };

            contextRunner.withBean("customIndexManager", IndexManager.class,
                    () -> customManager)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customIndexManager");
                        assertThat(context).doesNotHaveBean("indexManager");
                        assertThat(context.getBean(IndexManager.class)).isSameAs(customManager);
                    });
        }
    }

    @Nested
    @DisplayName("查询治理测试")
    class QueryGovernanceTest {

        @Test
        @DisplayName("builder size 超过 max-size 时应在执行前失败")
        void searchShouldRejectBuilderSizeExceedingMaxSize() {
            ElasticsearchProperties properties = new ElasticsearchProperties();
            properties.getQuery().setMaxSize(100);
            ElasticsearchAutoConfiguration.DefaultElasticsearchOperations operations =
                    new ElasticsearchAutoConfiguration.DefaultElasticsearchOperations(
                            org.mockito.Mockito.mock(ElasticsearchTemplate.class), properties);

            assertThatThrownBy(() -> operations.search("orders",
                    com.microservice.framework.elasticsearch.api.SearchQueryBuilder.create().size(101),
                    Object.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("max-size");
        }

        @Test
        @DisplayName("Pageable size 超过 max-size 时应在执行前失败")
        void searchShouldRejectPageableSizeExceedingMaxSize() {
            ElasticsearchProperties properties = new ElasticsearchProperties();
            properties.getQuery().setMaxSize(100);
            ElasticsearchAutoConfiguration.DefaultElasticsearchOperations operations =
                    new ElasticsearchAutoConfiguration.DefaultElasticsearchOperations(
                            org.mockito.Mockito.mock(ElasticsearchTemplate.class), properties);

            assertThatThrownBy(() -> operations.search("orders",
                    com.microservice.framework.elasticsearch.api.SearchQueryBuilder.create(),
                    PageRequest.of(0, 101),
                    Object.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("max-size");
        }

        @Test
        @DisplayName("Pageable offset + size 超过 max-from-size 时应在执行前失败")
        void searchShouldRejectPageableDeepPagination() {
            ElasticsearchProperties properties = new ElasticsearchProperties();
            properties.getQuery().setMaxFromSize(100);
            ElasticsearchAutoConfiguration.DefaultElasticsearchOperations operations =
                    new ElasticsearchAutoConfiguration.DefaultElasticsearchOperations(
                            org.mockito.Mockito.mock(ElasticsearchTemplate.class), properties);

            assertThatThrownBy(() -> operations.search("orders",
                    com.microservice.framework.elasticsearch.api.SearchQueryBuilder.create(),
                    PageRequest.of(10, 10),
                    Object.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("max-from-size");
        }
    }
}
