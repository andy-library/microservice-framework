package com.microservice.framework.elasticsearch.api;

/**
 * Elasticsearch 索引管理接口，提供索引的创建、删除、映射更新和刷新能力。
 *
 * <p>本接口封装 Spring Data Elasticsearch 的索引操作 API，
 * 提供统一的索引生命周期管理入口，屏蔽底层 API 变更。
 *
 * <p>实现类由 {@code ElasticsearchAutoConfiguration} 自动注册，
 * 用户可通过自定义 Bean 覆盖默认实现。
 *
 * @see ElasticsearchOperations
 */
public interface IndexManager {

    /**
     * 创建索引。
     *
     * <p>若索引已存在，将抛出 {@code FrameworkException}。
     *
     * @param indexName 索引名称
     * @return 操作是否成功
     */
    boolean createIndex(String indexName);

    /**
     * 创建索引并指定映射。
     *
     * <p>若索引已存在，将抛出 {@code FrameworkException}。
     *
     * @param indexName 索引名称
     * @param mapping   索引映射定义（JSON 格式）
     * @return 操作是否成功
     */
    boolean createIndex(String indexName, String mapping);

    /**
     * 删除索引。
     *
     * <p>若索引不存在，将抛出 {@code FrameworkException}。
     *
     * @param indexName 紹引名称
     * @return 操作是否成功
     */
    boolean deleteIndex(String indexName);

    /**
     * 检查索引是否存在。
     *
     * @param indexName 索引名称
     * @return 索引是否存在
     */
    boolean indexExists(String indexName);

    /**
     * 刷新索引，使最近的索引操作可被搜索。
     *
     * @param indexName 索引名称
     */
    void refreshIndex(String indexName);

    /**
     * 更新索引映射。
     *
     * <p>若索引不存在，将抛出 {@code FrameworkException}。
     *
     * @param indexName 索引名称
     * @param mapping   新增映射定义（JSON 格式）
     */
    void putMapping(String indexName, String mapping);

    /**
     * 检查 alias 是否存在。
     *
     * @param aliasName alias 名称
     * @return alias 是否存在
     */
    boolean aliasExists(String aliasName);

    /**
     * 为索引创建 alias。
     *
     * @param indexName 索引名称
     * @param aliasName alias 名称
     * @return 操作是否成功
     */
    boolean createAlias(String indexName, String aliasName);

    /**
     * 将 alias 从旧索引切换到新索引。
     *
     * @param aliasName alias 名称
     * @param fromIndex 旧索引
     * @param toIndex   新索引
     * @return 操作是否成功
     */
    boolean switchAlias(String aliasName, String fromIndex, String toIndex);
}
