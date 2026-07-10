package com.microservice.framework.elasticsearch.api;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Elasticsearch 操作接口，提供文档的搜索、索引、删除、批量操作和计数能力。
 *
 * <p>本接口封装 Spring Data Elasticsearch 的 {@code ElasticsearchOperations}，
 * 提供统一的文档 CRUD 和搜索操作入口，屏蔽底层 API 变更。
 *
 * <p>实现类由 {@code ElasticsearchAutoConfiguration} 自动注册，
 * 用户可通过自定义 Bean 覆盖默认实现。
 *
 * @see IndexManager
 * @see SearchQueryBuilder
 */
public interface ElasticsearchOperations {

    /**
     * 索引单个文档。
     *
     * @param indexName 索引名称
     * @param document  文档对象
     * @param id        文档 ID
     * @return 索引操作结果，包含文档 ID 和版本号
     */
    String index(String indexName, Object document, String id);

    /**
     * 根据 ID 获取单个文档。
     *
     * @param indexName 索引名称
     * @param id        文档 ID
     * @param clazz     文档类型
     * @return 查询到的文档，若不存在返回空 Optional
     */
    <T> Optional<T> get(String indexName, String id, Class<T> clazz);

    /**
     * 根据 ID 删除单个文档。
     *
     * @param indexName 索引名称
     * @param id        文档 ID
     * @return 删除操作结果，包含被删除文档 ID
     */
    String delete(String indexName, String id);

    /**
     * 根据查询条件搜索文档。
     *
     * @param indexName 索引名称
     * @param builder   搜索查询构建器
     * @param clazz     文档类型
     * @return 搜索结果列表
     */
    <T> List<T> search(String indexName, SearchQueryBuilder builder, Class<T> clazz);

    /**
     * 根据查询条件搜索文档（分页）。
     *
     * @param indexName 索引名称
     * @param builder   搜索查询构建器
     * @param pageable  分页参数
     * @param clazz     文档类型
     * @return 搜索结果列表
     */
    <T> List<T> search(String indexName, SearchQueryBuilder builder, Pageable pageable, Class<T> clazz);

    /**
     * 批量索引文档。
     *
     * @param indexName 索引名称
     * @param documents 文档列表，每个文档需包含 ID 字段
     * @return 批量操作结果，包含成功和失败的文档 ID
     */
    BulkResult bulkIndex(String indexName, List<?> documents);

    /**
     * 批量删除文档。
     *
     * @param indexName 索引名称
     * @param ids       待删除的文档 ID 列表
     * @return 批量操作结果，包含成功和失败的文档 ID
     */
    BulkResult bulkDelete(String indexName, List<String> ids);

    /**
     * 根据查询条件统计文档数量。
     *
     * @param indexName 索引名称
     * @param builder   搜索查询构建器
     * @return 匹配查询条件的文档数量
     */
    long count(String indexName, SearchQueryBuilder builder);

    /**
     * 批量操作结果。
     *
     * @param successfulIds 操作成功的文档 ID 列表
     * @param failedIds     操作失败的文档 ID 列表
     * @param failedItems   失败项及其错误信息
     */
    record BulkResult(List<String> successfulIds, List<String> failedIds,
                      Map<String, String> failedItems) {
        /**
         * 判断批量操作是否全部成功。
         *
         * @return 若无失败项则返回 {@code true}
         */
        public boolean isAllSuccess() {
            return failedIds.isEmpty();
        }
    }
}
