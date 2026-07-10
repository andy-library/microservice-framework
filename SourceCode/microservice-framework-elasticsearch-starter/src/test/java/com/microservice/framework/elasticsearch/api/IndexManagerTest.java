package com.microservice.framework.elasticsearch.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IndexManager 接口契约测试。
 *
 * <p>验证接口方法签名和行为约定，确保所有实现类满足 SPI 契约。
 */
class IndexManagerTest {

    /**
     * 最小实现，用于验证接口契约。
     */
    private static final IndexManager STUB = new IndexManager() {

        @Override
        public boolean createIndex(String indexName) {
            return true;
        }

        @Override
        public boolean createIndex(String indexName, String mapping) {
            return true;
        }

        @Override
        public boolean deleteIndex(String indexName) {
            return true;
        }

        @Override
        public boolean indexExists(String indexName) {
            return false;
        }

        @Override
        public void refreshIndex(String indexName) {
            // no-op
        }

        @Override
        public void putMapping(String indexName, String mapping) {
            // no-op
        }

        @Override
        public boolean aliasExists(String aliasName) {
            return "orders_current".equals(aliasName);
        }

        @Override
        public boolean createAlias(String indexName, String aliasName) {
            return true;
        }

        @Override
        public boolean switchAlias(String aliasName, String fromIndex, String toIndex) {
            return true;
        }
    };

    @Nested
    @DisplayName("接口方法存在性验证")
    class MethodExistenceTest {

        @Test
        @DisplayName("createIndex() 方法应可调用并返回 boolean")
        void createIndexShouldBeCallable() {
            boolean result = STUB.createIndex("test-index");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("createIndex() 带 mapping 方法应可调用")
        void createIndexWithMappingShouldBeCallable() {
            boolean result = STUB.createIndex("test-index", "{\"properties\":{}}");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("deleteIndex() 方法应可调用并返回 boolean")
        void deleteIndexShouldBeCallable() {
            boolean result = STUB.deleteIndex("test-index");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("indexExists() 方法应可调用并返回 boolean")
        void indexExistsShouldBeCallable() {
            boolean result = STUB.indexExists("test-index");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("refreshIndex() 方法应可调用")
        void refreshIndexShouldBeCallable() {
            STUB.refreshIndex("test-index");
            // 无返回值，仅验证方法可调用
        }

        @Test
        @DisplayName("putMapping() 方法应可调用")
        void putMappingShouldBeCallable() {
            STUB.putMapping("test-index", "{\"properties\":{}}");
            // 无返回值，仅验证方法可调用
        }

        @Test
        @DisplayName("aliasExists() 方法应可调用")
        void aliasExistsShouldBeCallable() {
            assertThat(STUB.aliasExists("orders_current")).isTrue();
        }

        @Test
        @DisplayName("createAlias() 方法应可调用")
        void createAliasShouldBeCallable() {
            assertThat(STUB.createAlias("orders_v1", "orders_current")).isTrue();
        }

        @Test
        @DisplayName("switchAlias() 方法应可调用")
        void switchAliasShouldBeCallable() {
            assertThat(STUB.switchAlias("orders_current", "orders_v1", "orders_v2")).isTrue();
        }
    }
}
