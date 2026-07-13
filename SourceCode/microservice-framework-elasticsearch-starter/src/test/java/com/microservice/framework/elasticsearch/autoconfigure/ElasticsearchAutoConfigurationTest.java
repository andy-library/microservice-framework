package com.microservice.framework.elasticsearch.autoconfigure;

import com.microservice.framework.elasticsearch.ElasticsearchProperties;
import com.microservice.framework.elasticsearch.api.ElasticsearchOperations;
import com.microservice.framework.elasticsearch.api.IndexManager;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        @DisplayName("连接配置应创建带 uri、认证和超时的 ClientConfiguration")
        void connectionPropertiesShouldCreateClientConfiguration() {
            contextRunner.withPropertyValues(
                            "framework.elasticsearch.connection.uris[0]=https://es-a.example.com:9243",
                            "framework.elasticsearch.connection.username=elastic",
                            "framework.elasticsearch.connection.password=secret",
                            "framework.elasticsearch.connection.connect-timeout=1234",
                            "framework.elasticsearch.connection.socket-timeout=5678")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(ClientConfiguration.class);
                        ClientConfiguration configuration = context.getBean(ClientConfiguration.class);
                        assertThat(configuration.useSsl()).isTrue();
                        assertThat(configuration.getEndpoints())
                                .extracting(endpoint -> endpoint.getHostString() + ":" + endpoint.getPort())
                                .containsExactly("es-a.example.com:9243");
                        assertThat(configuration.getConnectTimeout()).isEqualTo(Duration.ofMillis(1234));
                        assertThat(configuration.getSocketTimeout()).isEqualTo(Duration.ofMillis(5678));
                        assertThat(configuration.getDefaultHeaders().get("Authorization"))
                                .isNotEmpty();
                    });
        }

        @Test
        @DisplayName("启用 starter 但缺少 ElasticsearchTemplate 时应启动期 fail fast")
        void missingClientTemplateShouldFailFast() {
            contextRunner.withPropertyValues("framework.elasticsearch.fail-fast-client=true")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("ElasticsearchTemplate is not available");
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

        @Test
        @DisplayName("生产环境不允许创建无 mapping 索引")
        void prodShouldRejectMappinglessIndexCreation() {
            ElasticsearchTemplate template = mock(ElasticsearchTemplate.class);
            IndexOperations indexOperations = mock(IndexOperations.class);
            when(template.indexOps(any(IndexCoordinates.class))).thenReturn(indexOperations);
            when(indexOperations.exists()).thenReturn(false);
            when(indexOperations.create()).thenReturn(true);

            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.elasticsearch.connection.uris[0]=http://es-prod:9200",
                            "framework.elasticsearch.index.refresh-policy=WAIT_UNTIL")
                    .withBean(ElasticsearchTemplate.class, () -> template)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        IndexManager indexManager = context.getBean(IndexManager.class);
                        assertThatThrownBy(() -> indexManager.createIndex("orders"))
                                .isInstanceOf(com.microservice.framework.common.error.FrameworkException.class)
                                .hasMessageContaining("mapping is required");
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
                            null, properties);

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
                            null, properties);

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
                            null, properties);

            assertThatThrownBy(() -> operations.search("orders",
                    com.microservice.framework.elasticsearch.api.SearchQueryBuilder.create(),
                    PageRequest.of(10, 10),
                    Object.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("max-from-size");
        }

        @Test
        @DisplayName("builder 查询应保留精确 from 偏移并应用排序")
        @SuppressWarnings("unchecked")
        void builderSearchShouldHonorExactOffsetAndSorts() {
            ElasticsearchProperties properties = new ElasticsearchProperties();
            properties.getIndex().setRefreshPolicy("WAIT_UNTIL");
            ElasticsearchTemplate template = mock(ElasticsearchTemplate.class);
            SearchHits<TestDocument> searchHits = mock(SearchHits.class);
            when(searchHits.getSearchHits()).thenReturn(List.of());
            when(template.search(any(Query.class), eq(TestDocument.class), any(IndexCoordinates.class)))
                    .thenReturn(searchHits);
            ElasticsearchAutoConfiguration.DefaultElasticsearchOperations operations =
                    new ElasticsearchAutoConfiguration.DefaultElasticsearchOperations(template, properties);

            operations.search("orders", SearchQueryBuilder.create()
                    .term("tenantId", "t1")
                    .sort("createdAt", SearchQueryBuilder.SortDirection.DESC)
                    .from(15)
                    .size(10), TestDocument.class);

            org.mockito.ArgumentCaptor<Query> queryCaptor = org.mockito.ArgumentCaptor.forClass(Query.class);
            verify(template).search(queryCaptor.capture(), eq(TestDocument.class), any(IndexCoordinates.class));
            Query query = queryCaptor.getValue();
            assertThat(query.getPageable().getOffset()).isEqualTo(15);
            assertThat(query.getPageable().getPageSize()).isEqualTo(10);
            assertThat(query.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("bulkIndex 应按配置拆分批次并报告批次内失败项")
        void bulkIndexShouldUseBoundedBatchesAndReportItemFailures() {
            ElasticsearchProperties properties = new ElasticsearchProperties();
            properties.getBulk().setBatchSize(2);
            properties.getIndex().setRefreshPolicy("WAIT_UNTIL");
            ElasticsearchTemplate template = mock(ElasticsearchTemplate.class);
            when(template.bulkIndex(any(List.class), any(IndexCoordinates.class)))
                    .thenReturn(List.of(new IndexedObjectInformation("a", "orders", null, null, null)))
                    .thenThrow(new IllegalStateException("bulk shard rejected"));
            ElasticsearchAutoConfiguration.DefaultElasticsearchOperations operations =
                    new ElasticsearchAutoConfiguration.DefaultElasticsearchOperations(template, properties);

            ElasticsearchOperations.BulkResult result = operations.bulkIndex("orders",
                    List.of(new TestDocument("a"), new TestDocument("b"), new TestDocument("c")));

            org.mockito.ArgumentCaptor<List> batchCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
            verify(template, times(2)).bulkIndex(batchCaptor.capture(), any(IndexCoordinates.class));
            assertThat(batchCaptor.getAllValues()).extracting(List::size).containsExactly(2, 1);
            assertThat(result.successfulIds()).containsExactly("a");
            assertThat(result.failedIds()).containsExactly("c");
            assertThat(result.failedItems()).containsEntry("c", "bulk shard rejected");
        }
    }

    record TestDocument(String id) {
    }
}
