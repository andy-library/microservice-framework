package com.microservice.framework.elasticsearch.api;

import com.microservice.framework.elasticsearch.api.ElasticsearchOperations.BulkResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ElasticsearchOperations 接口契约测试。
 *
 * <p>验证接口方法签名和行为约定，确保所有实现类满足 SPI 契约。
 */
class ElasticsearchOperationsTest {

    /**
     * 最小实现，用于验证接口契约。
     */
    private static final ElasticsearchOperations STUB = new ElasticsearchOperations() {

        @Override
        public String index(String indexName, Object document, String id) {
            return id;
        }

        @Override
        public <T> Optional<T> get(String indexName, String id, Class<T> clazz) {
            return Optional.empty();
        }

        @Override
        public String delete(String indexName, String id) {
            return id;
        }

        @Override
        public <T> List<T> search(String indexName, SearchQueryBuilder builder, Class<T> clazz) {
            return List.of();
        }

        @Override
        public <T> List<T> search(String indexName, SearchQueryBuilder builder,
                                  org.springframework.data.domain.Pageable pageable, Class<T> clazz) {
            return List.of();
        }

        @Override
        public BulkResult bulkIndex(String indexName, List<?> documents) {
            return new BulkResult(List.of(), List.of(), Map.of());
        }

        @Override
        public BulkResult bulkDelete(String indexName, List<String> ids) {
            return new BulkResult(List.of(), List.of(), Map.of());
        }

        @Override
        public long count(String indexName, SearchQueryBuilder builder) {
            return 0L;
        }
    };

    @Nested
    @DisplayName("接口方法存在性验证")
    class MethodExistenceTest {

        @Test
        @DisplayName("index() 方法应可调用并返回文档 ID")
        void indexMethodShouldBeCallable() {
            String result = STUB.index("test-index", new Object(), "doc-1");
            assertThat(result).isEqualTo("doc-1");
        }

        @Test
        @DisplayName("get() 方法应可调用并返回 Optional")
        void getMethodShouldBeCallable() {
            Optional<Object> result = STUB.get("test-index", "doc-1", Object.class);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("delete() 方法应可调用并返回被删除文档 ID")
        void deleteMethodShouldBeCallable() {
            String result = STUB.delete("test-index", "doc-1");
            assertThat(result).isEqualTo("doc-1");
        }

        @Test
        @DisplayName("search() 方法应可调用并返回列表")
        void searchMethodShouldBeCallable() {
            List<Object> result = STUB.search("test-index",
                    SearchQueryBuilder.create(), Object.class);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("search() 分页方法应可调用")
        void searchWithPageableShouldBeCallable() {
            List<Object> result = STUB.search("test-index",
                    SearchQueryBuilder.create(),
                    org.springframework.data.domain.Pageable.ofSize(10), Object.class);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("bulkIndex() 方法应可调用并返回 BulkResult")
        void bulkIndexMethodShouldBeCallable() {
            BulkResult result = STUB.bulkIndex("test-index", List.of());
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("bulkDelete() 方法应可调用并返回 BulkResult")
        void bulkDeleteMethodShouldBeCallable() {
            BulkResult result = STUB.bulkDelete("test-index", List.of());
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("count() 方法应可调用并返回计数")
        void countMethodShouldBeCallable() {
            long result = STUB.count("test-index", SearchQueryBuilder.create());
            assertThat(result).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("BulkResult 契约测试")
    class BulkResultTest {

        @Test
        @DisplayName("BulkResult 全部成功时 isAllSuccess() 应返回 true")
        void allSuccessShouldReturnTrue() {
            BulkResult result = new BulkResult(List.of("id1", "id2"), List.of(), Map.of());
            assertThat(result.isAllSuccess()).isTrue();
        }

        @Test
        @DisplayName("BulkResult 有失败项时 isAllSuccess() 应返回 false")
        void withFailuresShouldReturnFalse() {
            BulkResult result = new BulkResult(
                    List.of("id1"), List.of("id2"), Map.of("id2", "error"));
            assertThat(result.isAllSuccess()).isFalse();
        }

        @Test
        @DisplayName("BulkResult 应包含成功和失败 ID 列表")
        void bulkResultShouldContainIds() {
            BulkResult result = new BulkResult(
                    List.of("id1", "id3"), List.of("id2"), Map.of("id2", "timeout"));
            assertThat(result.successfulIds()).containsExactly("id1", "id3");
            assertThat(result.failedIds()).containsExactly("id2");
            assertThat(result.failedItems()).containsEntry("id2", "timeout");
        }
    }
}
