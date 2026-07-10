package com.microservice.framework.elasticsearch.autoconfigure;

import com.microservice.framework.elasticsearch.ElasticsearchProperties;
import com.microservice.framework.elasticsearch.api.ElasticsearchOperations;
import com.microservice.framework.elasticsearch.api.IndexManager;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.BoolClause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.MatchClause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.RangeClause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.TermClause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.Clause;
import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Elasticsearch 自动配置类。
 *
 * <p>当 Spring Data Elasticsearch 在类路径上且 {@code framework.elasticsearch.enabled=true}（默认）
 * 时，自动注册 {@link ElasticsearchOperations} 和 {@link IndexManager} Bean。
 *
 * <p>用户可通过自定义同类型 Bean 覆盖默认实现。
 *
 * <p>配置前缀：{@code framework.elasticsearch}
 *
 * @see ElasticsearchOperations
 * @see IndexManager
 * @see ElasticsearchProperties
 */
@AutoConfiguration
@EnableConfigurationProperties(ElasticsearchProperties.class)
@ConditionalOnClass(ElasticsearchTemplate.class)
@ConditionalOnProperty(prefix = "framework.elasticsearch", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ElasticsearchAutoConfiguration {

    private static final String MODULE = "ES";

    private final ElasticsearchProperties properties;

    public ElasticsearchAutoConfiguration(ElasticsearchProperties properties) {
        this.properties = properties;
    }

    /**
     * 注册 ElasticsearchOperations 默认实现。
     *
     * <p>用户可通过自定义 {@link ElasticsearchOperations} Bean 覆盖此默认实现。
     *
     * <p>使用 {@link ObjectProvider} 使得 {@code ElasticsearchTemplate} 在上下文中
     * 缺失时（如测试环境）仍能创建 Bean，此时操作方法会因模板为空而抛出异常，
     * 但 Bean 本身可以正常注册和发现。
     *
     * @param elasticsearchTemplateProvider Spring Data Elasticsearch 提供的 ElasticsearchTemplate（可选）
     * @return DefaultElasticsearchOperations 实例
     */
    @Bean("elasticsearchOperations")
    @ConditionalOnMissingBean(ElasticsearchOperations.class)
    public ElasticsearchOperations elasticsearchOperations(
            ObjectProvider<ElasticsearchTemplate> elasticsearchTemplateProvider) {
        return new DefaultElasticsearchOperations(elasticsearchTemplateProvider.getIfAvailable(), properties);
    }

    /**
     * 注册 IndexManager 默认实现。
     *
     * <p>用户可通过自定义 {@link IndexManager} Bean 覆盖此默认实现。
     *
     * <p>使用 {@link ObjectProvider} 使得 {@code ElasticsearchTemplate} 在上下文中
     * 缺失时（如测试环境）仍能创建 Bean，此时操作方法会因模板为空而抛出异常，
     * 但 Bean 本身可以正常注册和发现。
     *
     * @param elasticsearchTemplateProvider Spring Data Elasticsearch 提供的 ElasticsearchTemplate（可选）
     * @return DefaultIndexManager 实例
     */
    @Bean("indexManager")
    @ConditionalOnMissingBean(IndexManager.class)
    public IndexManager indexManager(
            ObjectProvider<ElasticsearchTemplate> elasticsearchTemplateProvider) {
        return new DefaultIndexManager(elasticsearchTemplateProvider.getIfAvailable());
    }

    /**
     * 生产环境准入校验。
     *
     * <p>ES 属于共享搜索集群能力，生产环境必须避免连接本地节点、自动建索引、
     * 高频强制刷新以及放开深分页窗口。
     *
     * @param environment Spring 环境
     * @return SmartInitializingSingleton
     */
    @Bean
    @ConditionalOnMissingBean(name = "elasticsearchProductionSafetyValidator")
    public SmartInitializingSingleton elasticsearchProductionSafetyValidator(Environment environment) {
        return () -> {
            if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                return;
            }
            for (String uri : properties.getConnection().getUris()) {
                if (isLocalUri(uri)) {
                    throw new FrameworkException(
                            FrameworkErrorCode.of(MODULE, "GOVERNANCE", 1),
                            "local Elasticsearch uri is not allowed in prod profile: " + uri);
                }
            }
            if (properties.getIndex().isAutoCreate()) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "GOVERNANCE", 2),
                        "framework.elasticsearch.index.auto-create cannot be enabled in prod profile");
            }
            if ("IMMEDIATE".equalsIgnoreCase(properties.getIndex().getRefreshPolicy())) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "GOVERNANCE", 3),
                        "IMMEDIATE refresh policy is not allowed in prod profile");
            }
            if (properties.getQuery().getMaxFromSize() > 10000) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "GOVERNANCE", 4),
                        "framework.elasticsearch.query.max-from-size must not exceed 10000 in prod profile");
            }
        };
    }

    private boolean isLocalUri(String uri) {
        if (uri == null) {
            return true;
        }
        String normalized = uri.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("://localhost")
                || normalized.contains("://127.")
                || normalized.contains("://0.0.0.0")
                || normalized.contains("://[::1]");
    }

    // ========================================================================
    // 包私有的默认实现类
    // ========================================================================

    /**
     * ElasticsearchOperations 默认实现，基于 Spring Data Elasticsearch 的 {@code ElasticsearchTemplate}。
     *
     * <p>此类为包私有的内部静态类，由 {@code ElasticsearchAutoConfiguration} 注册为 Bean，
     * 用户可通过自定义 {@code ElasticsearchOperations} Bean 覆盖。
     */
    static class DefaultElasticsearchOperations implements ElasticsearchOperations {

        private final ElasticsearchTemplate elasticsearchTemplate;
        private final ElasticsearchProperties properties;

        DefaultElasticsearchOperations(ElasticsearchTemplate elasticsearchTemplate,
                                       ElasticsearchProperties properties) {
            this.elasticsearchTemplate = elasticsearchTemplate;
            this.properties = properties;
        }

        @Override
        public String index(String indexName, Object document, String id) {
            requireTemplate();
            validateName(indexName, "indexName");
            validateName(id, "id");
            if (document == null) {
                throw new IllegalArgumentException("document must not be null");
            }
            org.springframework.data.elasticsearch.core.query.IndexQuery indexQuery =
                    new IndexQueryBuilder()
                            .withId(id)
                            .withObject(document)
                            .withIndex(indexName)
                            .build();
            String documentId = elasticsearchTemplate.index(indexQuery, IndexCoordinates.of(indexName));
            if (documentId == null) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "INDEX", 1),
                        "Failed to index document into '" + indexName + "' with id '" + id + "'");
            }
            refreshIfImmediate(indexName);
            return documentId;
        }

        @Override
        public <T> Optional<T> get(String indexName, String id, Class<T> clazz) {
            requireTemplate();
            validateName(indexName, "indexName");
            validateName(id, "id");
            if (clazz == null) {
                throw new IllegalArgumentException("clazz must not be null");
            }
            T document = elasticsearchTemplate.get(id, clazz, IndexCoordinates.of(indexName));
            return Optional.ofNullable(document);
        }

        @Override
        public String delete(String indexName, String id) {
            requireTemplate();
            validateName(indexName, "indexName");
            validateName(id, "id");
            String deletedId = elasticsearchTemplate.delete(id, IndexCoordinates.of(indexName));
            if (deletedId == null) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "DELETE", 1),
                        "Failed to delete document from '" + indexName + "' with id '" + id + "'");
            }
            return deletedId;
        }

        @Override
        public <T> List<T> search(String indexName, SearchQueryBuilder builder, Class<T> clazz) {
            validateSearchArguments(indexName, builder, clazz);
            validateBuilderGovernance(builder);
            requireTemplate();
            org.springframework.data.elasticsearch.core.query.Query query = buildQuery(builder);
            org.springframework.data.elasticsearch.core.SearchHits<T> searchHits =
                    elasticsearchTemplate.search(query, clazz, IndexCoordinates.of(indexName));
            return searchHits.getSearchHits().stream()
                    .map(org.springframework.data.elasticsearch.core.SearchHit::getContent)
                    .toList();
        }

        @Override
        public <T> List<T> search(String indexName, SearchQueryBuilder builder,
                                  org.springframework.data.domain.Pageable pageable, Class<T> clazz) {
            validateSearchArguments(indexName, builder, clazz);
            if (pageable == null) {
                throw new IllegalArgumentException("pageable must not be null");
            }
            validatePageable(pageable);
            validateBuilderGovernance(builder);
            requireTemplate();
            org.springframework.data.elasticsearch.core.query.Query query =
                    buildQuery(builder).setPageable(pageable);
            org.springframework.data.elasticsearch.core.SearchHits<T> searchHits =
                    elasticsearchTemplate.search(query, clazz, IndexCoordinates.of(indexName));
            return searchHits.getSearchHits().stream()
                    .map(org.springframework.data.elasticsearch.core.SearchHit::getContent)
                    .toList();
        }

        @Override
        public BulkResult bulkIndex(String indexName, List<?> documents) {
            requireTemplate();
            validateName(indexName, "indexName");
            if (documents == null || documents.isEmpty()) {
                throw new IllegalArgumentException("documents must not be null or empty");
            }
            List<org.springframework.data.elasticsearch.core.query.IndexQuery> queries = new ArrayList<>();
            for (Object document : documents) {
                org.springframework.data.elasticsearch.core.query.IndexQuery indexQuery =
                        new IndexQueryBuilder()
                                .withObject(document)
                                .withIndex(indexName)
                                .build();
                queries.add(indexQuery);
            }

            List<String> successfulIds = new ArrayList<>();
            List<String> failedIds = new ArrayList<>();
            Map<String, String> failedItems = new HashMap<>();

            List<org.springframework.data.elasticsearch.core.IndexedObjectInformation> results =
                    elasticsearchTemplate.bulkIndex(queries, IndexCoordinates.of(indexName));

            for (org.springframework.data.elasticsearch.core.IndexedObjectInformation result : results) {
                String id = result.id();
                if (id != null) {
                    successfulIds.add(id);
                } else {
                    failedIds.add("unknown");
                    failedItems.put("unknown", "Index operation returned null id");
                }
            }
            refreshIfImmediate(indexName);

            return new BulkResult(successfulIds, failedIds, failedItems);
        }

        private void refreshIfImmediate(String indexName) {
            if ("IMMEDIATE".equalsIgnoreCase(properties.getIndex().getRefreshPolicy())) {
                elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName)).refresh();
            }
        }

        @Override
        public BulkResult bulkDelete(String indexName, List<String> ids) {
            requireTemplate();
            validateName(indexName, "indexName");
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("ids must not be null or empty");
            }
            List<String> successfulIds = new ArrayList<>();
            List<String> failedIds = new ArrayList<>();
            Map<String, String> failedItems = new HashMap<>();
            IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);

            for (String id : ids) {
                validateName(id, "id");
                try {
                    String deletedId = elasticsearchTemplate.delete(id, indexCoordinates);
                    if (deletedId != null) {
                        successfulIds.add(deletedId);
                    } else {
                        failedIds.add(id);
                        failedItems.put(id, "Document not found");
                    }
                } catch (Exception e) {
                    failedIds.add(id);
                    failedItems.put(id, e.getMessage());
                }
            }

            return new BulkResult(successfulIds, failedIds, failedItems);
        }

        @Override
        public long count(String indexName, SearchQueryBuilder builder) {
            requireTemplate();
            validateName(indexName, "indexName");
            if (builder == null) {
                throw new IllegalArgumentException("builder must not be null");
            }
            org.springframework.data.elasticsearch.core.query.Query query = buildQuery(builder);
            return elasticsearchTemplate.count(query, Object.class, IndexCoordinates.of(indexName));
        }

        private org.springframework.data.elasticsearch.core.query.Query buildQuery(
                SearchQueryBuilder builder) {
            builder.validateSize(properties.getQuery().getMaxSize());
            builder.validateFromSize(properties.getQuery().getMaxFromSize());
            String queryJson = buildQueryJson(builder);
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                    builder.getFrom() / builder.getSize(), builder.getSize());
            return new org.springframework.data.elasticsearch.core.query.StringQuery(queryJson, pageable);
        }

        private void validatePageable(org.springframework.data.domain.Pageable pageable) {
            if (pageable.getPageSize() > properties.getQuery().getMaxSize()) {
                throw new IllegalArgumentException("Elasticsearch pageable size is too large: size = "
                        + pageable.getPageSize() + ", max-size = " + properties.getQuery().getMaxSize());
            }
            long fromSize = pageable.getOffset() + pageable.getPageSize();
            if (fromSize > properties.getQuery().getMaxFromSize()) {
                throw new IllegalArgumentException("Elasticsearch pageable deep pagination is not allowed: "
                        + "offset + size = " + fromSize
                        + ", max-from-size = " + properties.getQuery().getMaxFromSize());
            }
        }

        private String buildQueryJson(SearchQueryBuilder builder) {
            if (!builder.hasClauses()) {
                return "{\"match_all\":{}}";
            }

            Map<String, Object> queryMap = new LinkedHashMap<>();
            Map<String, Object> boolMap = new LinkedHashMap<>();
            List<Map<String, Object>> mustClauses = new ArrayList<>();
            List<Map<String, Object>> shouldClauses = new ArrayList<>();
            List<Map<String, Object>> mustNotClauses = new ArrayList<>();
            List<Map<String, Object>> standaloneClauses = new ArrayList<>();

            for (Clause clause : builder.getClauses()) {
                switch (clause) {
                    case MatchClause m -> {
                        Map<String, Object> matchParams = new LinkedHashMap<>();
                        matchParams.put("query", m.value());
                        if (m.analyzer() != null) {
                            matchParams.put("analyzer", m.analyzer());
                        }
                        standaloneClauses.add(Map.of("match", Map.of(m.field(), matchParams)));
                    }
                    case TermClause t -> {
                        standaloneClauses.add(Map.of("term",
                                Map.of(t.field(), Map.of("value", t.value()))));
                    }
                    case RangeClause r -> {
                        Map<String, Object> rangeParams = new LinkedHashMap<>();
                        if (r.from() != null) {
                            rangeParams.put(r.includeFrom() ? "gte" : "gt", r.from());
                        }
                        if (r.to() != null) {
                            rangeParams.put(r.includeTo() ? "lte" : "lt", r.to());
                        }
                        standaloneClauses.add(Map.of("range", Map.of(r.field(), rangeParams)));
                    }
                    case BoolClause b -> {
                        Map<String, Object> matchEntry = Map.of("match",
                                Map.of(b.field(), Map.of("query", b.value())));
                        switch (b.boolType()) {
                            case MUST -> mustClauses.add(matchEntry);
                            case SHOULD -> shouldClauses.add(matchEntry);
                            case MUST_NOT -> mustNotClauses.add(matchEntry);
                        }
                    }
                }
            }

            if (!mustClauses.isEmpty() || !shouldClauses.isEmpty() || !mustNotClauses.isEmpty()) {
                mustClauses.addAll(0, standaloneClauses);
                if (!mustClauses.isEmpty()) {
                    boolMap.put("must", mustClauses);
                }
                if (!shouldClauses.isEmpty()) {
                    boolMap.put("should", shouldClauses);
                }
                if (!mustNotClauses.isEmpty()) {
                    boolMap.put("must_not", mustNotClauses);
                }
                queryMap.put("bool", boolMap);
            } else if (standaloneClauses.size() == 1) {
                queryMap = standaloneClauses.get(0);
            } else {
                boolMap.put("must", standaloneClauses);
                queryMap.put("bool", boolMap);
            }

            return toJsonString(queryMap);
        }

        private String toJsonString(Map<String, Object> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(valueToJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }

        private String valueToJson(Object value) {
            if (value == null) {
                return "null";
            }
            if (value instanceof String) {
                return "\"" + escapeJson((String) value) + "\"";
            }
            if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            if (value instanceof Map) {
                return toJsonString((Map<String, Object>) value);
            }
            if (value instanceof List) {
                StringBuilder sb = new StringBuilder("[");
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(valueToJson(list.get(i)));
                }
                sb.append("]");
                return sb.toString();
            }
            return "\"" + escapeJson(value.toString()) + "\"";
        }

        private String escapeJson(String s) {
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        private void validateSearchArguments(String indexName, SearchQueryBuilder builder, Class<?> clazz) {
            validateName(indexName, "indexName");
            if (builder == null) {
                throw new IllegalArgumentException("builder must not be null");
            }
            if (clazz == null) {
                throw new IllegalArgumentException("clazz must not be null");
            }
        }

        private void validateBuilderGovernance(SearchQueryBuilder builder) {
            builder.validateSize(properties.getQuery().getMaxSize());
            builder.validateFromSize(properties.getQuery().getMaxFromSize());
        }

        private void validateName(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
        }

        private void requireTemplate() {
            if (elasticsearchTemplate == null) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "CLIENT", 1),
                        "ElasticsearchTemplate is not available; check Elasticsearch client auto-configuration");
            }
        }
    }

    /**
     * IndexManager 默认实现，基于 Spring Data Elasticsearch 的 {@code ElasticsearchTemplate}。
     *
     * <p>此类为包私有的内部静态类，由 {@code ElasticsearchAutoConfiguration} 注册为 Bean，
     * 用户可通过自定义 {@code IndexManager} Bean 覆盖。
     */
    static class DefaultIndexManager implements IndexManager {

        private final ElasticsearchTemplate elasticsearchTemplate;

        DefaultIndexManager(ElasticsearchTemplate elasticsearchTemplate) {
            this.elasticsearchTemplate = elasticsearchTemplate;
        }

        @Override
        public boolean createIndex(String indexName) {
            requireTemplate();
            validateName(indexName, "indexName");
            org.springframework.data.elasticsearch.core.IndexOperations indexOps =
                    elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName));
            if (indexOps.exists()) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "INDEX", 2),
                        "Index '" + indexName + "' already exists");
            }
            return indexOps.create();
        }

        @Override
        public boolean createIndex(String indexName, String mapping) {
            requireTemplate();
            validateName(indexName, "indexName");
            org.springframework.data.elasticsearch.core.IndexOperations indexOps =
                    elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName));
            if (indexOps.exists()) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "INDEX", 2),
                        "Index '" + indexName + "' already exists");
            }
            boolean created = indexOps.create();
            if (created && mapping != null && !mapping.isBlank()) {
                Document mappingDoc = Document.parse(mapping);
                indexOps.putMapping(mappingDoc);
            }
            return created;
        }

        @Override
        public boolean deleteIndex(String indexName) {
            requireTemplate();
            validateName(indexName, "indexName");
            org.springframework.data.elasticsearch.core.IndexOperations indexOps =
                    elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName));
            if (!indexOps.exists()) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "INDEX", 3),
                        "Index '" + indexName + "' does not exist");
            }
            return indexOps.delete();
        }

        @Override
        public boolean indexExists(String indexName) {
            requireTemplate();
            validateName(indexName, "indexName");
            org.springframework.data.elasticsearch.core.IndexOperations indexOps =
                    elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName));
            return indexOps.exists();
        }

        @Override
        public void refreshIndex(String indexName) {
            requireTemplate();
            validateName(indexName, "indexName");
            org.springframework.data.elasticsearch.core.IndexOperations indexOps =
                    elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName));
            indexOps.refresh();
        }

        @Override
        public void putMapping(String indexName, String mapping) {
            requireTemplate();
            validateName(indexName, "indexName");
            if (mapping == null || mapping.isBlank()) {
                throw new IllegalArgumentException("mapping must not be blank");
            }
            org.springframework.data.elasticsearch.core.IndexOperations indexOps =
                    elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName));
            if (!indexOps.exists()) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "INDEX", 3),
                        "Index '" + indexName + "' does not exist");
            }
            Document mappingDoc = Document.parse(mapping);
            indexOps.putMapping(mappingDoc);
        }

        @Override
        public boolean aliasExists(String aliasName) {
            requireTemplate();
            validateName(aliasName, "aliasName");
            Map<String, java.util.Set<org.springframework.data.elasticsearch.core.index.AliasData>> aliases =
                    elasticsearchTemplate.indexOps(IndexCoordinates.of(aliasName)).getAliases(aliasName);
            return aliases.values().stream().anyMatch(values -> !values.isEmpty());
        }

        @Override
        public boolean createAlias(String indexName, String aliasName) {
            requireTemplate();
            validateName(indexName, "indexName");
            validateName(aliasName, "aliasName");
            AliasActionParameters parameters = AliasActionParameters.builder()
                    .withIndices(indexName)
                    .withAliases(aliasName)
                    .build();
            return elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName))
                    .alias(new AliasActions(new AliasAction.Add(parameters)));
        }

        @Override
        public boolean switchAlias(String aliasName, String fromIndex, String toIndex) {
            requireTemplate();
            validateName(aliasName, "aliasName");
            validateName(fromIndex, "fromIndex");
            validateName(toIndex, "toIndex");
            AliasActionParameters remove = AliasActionParameters.builder()
                    .withIndices(fromIndex)
                    .withAliases(aliasName)
                    .build();
            AliasActionParameters add = AliasActionParameters.builder()
                    .withIndices(toIndex)
                    .withAliases(aliasName)
                    .build();
            return elasticsearchTemplate.indexOps(IndexCoordinates.of(toIndex))
                    .alias(new AliasActions(new AliasAction.Remove(remove), new AliasAction.Add(add)));
        }

        private void validateName(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
        }

        private void requireTemplate() {
            if (elasticsearchTemplate == null) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "CLIENT", 1),
                        "ElasticsearchTemplate is not available; check Elasticsearch client auto-configuration");
            }
        }
    }
}
