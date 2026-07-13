package com.microservice.framework.elasticsearch;

import com.microservice.framework.elasticsearch.ElasticsearchProperties.ConnectionProperties;
import com.microservice.framework.elasticsearch.ElasticsearchProperties.IndexProperties;
import com.microservice.framework.elasticsearch.ElasticsearchProperties.QueryProperties;
import com.microservice.framework.elasticsearch.ElasticsearchProperties.BulkProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Elasticsearch Starter 配置属性。
 *
 * <p>配置前缀：{@code framework.elasticsearch}
 *
 * <p>默认情况下，Elasticsearch Starter 自动激活（{@code enabled} 默认为 {@code true}）。
 * 可通过设置 {@code framework.elasticsearch.enabled=false} 禁用整个 Starter。
 *
 * @see ConnectionProperties
 * @see IndexProperties
 * @see QueryProperties
 * @see BulkProperties
 */
@ConfigurationProperties(prefix = "framework.elasticsearch")
@Validated
public class ElasticsearchProperties {

    /** 是否启用 Elasticsearch Starter，默认 {@code true}。 */
    private boolean enabled = true;

    /** 启动期是否要求 ElasticsearchTemplate 必须可用，默认 {@code false}。 */
    private boolean failFastClient = false;

    /** 连接配置。 */
    @NestedConfigurationProperty
    private ConnectionProperties connection = new ConnectionProperties();

    /** 索引管理配置。 */
    @NestedConfigurationProperty
    private IndexProperties index = new IndexProperties();

    /** 查询配置。 */
    @NestedConfigurationProperty
    private QueryProperties query = new QueryProperties();

    /** 批量操作配置。 */
    @NestedConfigurationProperty
    private BulkProperties bulk = new BulkProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailFastClient() {
        return failFastClient;
    }

    public void setFailFastClient(boolean failFastClient) {
        this.failFastClient = failFastClient;
    }

    public ConnectionProperties getConnection() {
        return connection;
    }

    public void setConnection(ConnectionProperties connection) {
        this.connection = connection;
    }

    public IndexProperties getIndex() {
        return index;
    }

    public void setIndex(IndexProperties index) {
        this.index = index;
    }

    public QueryProperties getQuery() {
        return query;
    }

    public void setQuery(QueryProperties query) {
        this.query = query;
    }

    public BulkProperties getBulk() {
        return bulk;
    }

    public void setBulk(BulkProperties bulk) {
        this.bulk = bulk;
    }

    // ========================================================================
    // 连接配置
    // ========================================================================

    /**
     * Elasticsearch 连接配置。
     *
     * <p>包含 ES 集群连接地址、认证信息和超时设置。
     */
    public static class ConnectionProperties {

        /** ES 集群节点地址列表，默认 {@code ["http://localhost:9200"]}。 */
        private java.util.List<String> uris = java.util.List.of("http://localhost:9200");

        /** 认证用户名，默认空（无认证）。 */
        private String username = "";

        /** 认证密码，默认空（无认证）。 */
        private String password = "";

        /** 连接超时时间（毫秒），默认 {@code 5000}。 */
        @Min(1)
        private int connectTimeout = 5000;

        /** Socket 超时时间（毫秒），默认 {@code 30000}。 */
        @Min(1)
        private int socketTimeout = 30000;

        public java.util.List<String> getUris() {
            return uris;
        }

        public void setUris(java.util.List<String> uris) {
            this.uris = uris;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
        }
    }

    // ========================================================================
    // 索引管理配置
    // ========================================================================

    /**
     * Elasticsearch 索引管理配置。
     *
     * <p>控制索引的自动创建行为和刷新策略。
     */
    public static class IndexProperties {

        /** 是否自动创建索引，默认 {@code false}。 */
        private boolean autoCreate = false;

        /** 索引刷新策略，默认 {@code IMMEDIATE}。 */
        @NotBlank
        private String refreshPolicy = "IMMEDIATE";

        public boolean isAutoCreate() {
            return autoCreate;
        }

        public void setAutoCreate(boolean autoCreate) {
            this.autoCreate = autoCreate;
        }

        public String getRefreshPolicy() {
            return refreshPolicy;
        }

        public void setRefreshPolicy(String refreshPolicy) {
            this.refreshPolicy = refreshPolicy;
        }
    }

    // ========================================================================
    // 查询配置
    // ========================================================================

    /**
     * Elasticsearch 查询配置。
     *
     * <p>控制查询结果的默认分页大小和最大允许返回条数。
     */
    public static class QueryProperties {

        /** 查询默认返回条数，默认 {@code 10}。 */
        @Min(1)
        private int defaultSize = 10;

        /** 查询最大允许返回条数，默认 {@code 10000}。 */
        @Min(1)
        @Max(100000)
        private int maxSize = 10000;

        /** from + size 最大允许窗口，默认 10000，避免深分页拖垮集群。 */
        @Min(1)
        @Max(100000)
        private int maxFromSize = 10000;

        public int getDefaultSize() {
            return defaultSize;
        }

        public void setDefaultSize(int defaultSize) {
            this.defaultSize = defaultSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getMaxFromSize() {
            return maxFromSize;
        }

        public void setMaxFromSize(int maxFromSize) {
            this.maxFromSize = maxFromSize;
        }
    }

    // ========================================================================
    // 批量操作配置
    // ========================================================================

    /**
     * Elasticsearch 批量操作配置。
     *
     * <p>控制批量索引操作的批次大小和刷新间隔。
     */
    public static class BulkProperties {

        /** 批量操作每批大小，默认 {@code 1000}。 */
        @Min(1)
        @Max(10000)
        private int batchSize = 1000;

        /** 批量操作刷新间隔（毫秒），默认 {@code 5000}。 */
        @Min(100)
        private int flushInterval = 5000;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getFlushInterval() {
            return flushInterval;
        }

        public void setFlushInterval(int flushInterval) {
            this.flushInterval = flushInterval;
        }
    }
}
