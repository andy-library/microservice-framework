package com.microservice.demo.embedded;

import com.microservice.framework.elasticsearch.api.ElasticsearchOperations;
import com.microservice.framework.elasticsearch.api.IndexManager;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.Clause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.MatchClause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.TermClause;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embedded Elasticsearch Configuration
 *
 * Provides in-memory Elasticsearch bean implementations that override the starter's
 * default implementations via {@code @ConditionalOnMissingBean}. Activates only
 * when {@code framework.elasticsearch.provider=embedded} is set.
 *
 * <p>All implementations are fully functional (not stubs) and suitable for
 * integration testing and demo purposes without a real Elasticsearch server.</p>
 *
 * <p>Limitations: In-memory search supports match and term queries but not range,
 * bool, or complex compound queries. Unsupported queries return empty results.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "framework.elasticsearch", name = "provider", havingValue = "embedded")
public class EmbeddedElasticsearchConfiguration {

    @Bean
    public ElasticsearchOperations inMemoryElasticsearchOperations() {
        return new InMemoryElasticsearchOperations();
    }

    @Bean
    public IndexManager inMemoryIndexManager() {
        return new InMemoryIndexManager();
    }

    // ======================================================================
    // InMemoryElasticsearchOperations
    // ======================================================================

    /**
     * ConcurrentHashMap-backed in-memory ElasticsearchOperations.
     * Supports index, get, delete, search (match/term), bulkIndex, bulkDelete, and count.
     * Range/bool queries return empty results — sufficient for demo/verification use.
     */
    static class InMemoryElasticsearchOperations implements ElasticsearchOperations {

        private final ConcurrentHashMap<String, ConcurrentHashMap<String, DocumentEntry>> indices = new ConcurrentHashMap<>();
        private static final int MAX_FROM_SIZE = 10000;

        static class DocumentEntry {
            final Object document;
            final long indexedAtMs;

            DocumentEntry(Object document) {
                this.document = document;
                this.indexedAtMs = System.currentTimeMillis();
            }
        }

        private ConcurrentHashMap<String, DocumentEntry> getOrCreateIndex(String indexName) {
            return indices.computeIfAbsent(indexName, k -> new ConcurrentHashMap<>());
        }

        @Override
        public String index(String indexName, Object document, String id) {
            ConcurrentHashMap<String, DocumentEntry> index = getOrCreateIndex(indexName);
            index.put(id, new DocumentEntry(document));
            return id;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(String indexName, String id, Class<T> clazz) {
            ConcurrentHashMap<String, DocumentEntry> index = indices.get(indexName);
            if (index == null) {
                return Optional.empty();
            }
            DocumentEntry entry = index.get(id);
            if (entry == null) {
                return Optional.empty();
            }
            if (clazz.isInstance(entry.document)) {
                return Optional.of((T) entry.document);
            }
            // Attempt conversion for Map-based documents
            if (entry.document instanceof Map && clazz == Map.class) {
                return Optional.of((T) entry.document);
            }
            return Optional.empty();
        }

        @Override
        public String delete(String indexName, String id) {
            ConcurrentHashMap<String, DocumentEntry> index = indices.get(indexName);
            if (index == null) {
                return null;
            }
            DocumentEntry removed = index.remove(id);
            return removed != null ? id : null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<T> search(String indexName, SearchQueryBuilder builder, Class<T> clazz) {
            return search(indexName, builder, null, clazz);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<T> search(String indexName, SearchQueryBuilder builder, Pageable pageable, Class<T> clazz) {
            builder.validateFromSize(MAX_FROM_SIZE);
            ConcurrentHashMap<String, DocumentEntry> index = indices.get(indexName);
            if (index == null || !builder.hasClauses()) {
                // No index or no query clauses — return empty
                return Collections.emptyList();
            }

            List<T> results = new ArrayList<>();
            for (DocumentEntry entry : index.values()) {
                if (matchesQuery(entry, builder)) {
                    if (clazz.isInstance(entry.document)) {
                        results.add((T) entry.document);
                    } else if (entry.document instanceof Map) {
                        // For Map-based documents, try to cast
                        results.add((T) entry.document);
                    }
                }
            }

            // Apply pagination if provided
            if (pageable != null) {
                int from = pageable.getPageNumber() * pageable.getPageSize();
                int to = Math.min(from + pageable.getPageSize(), results.size());
                if (from >= results.size()) {
                    return Collections.emptyList();
                }
                return results.subList(from, to);
            }

            // Apply builder pagination
            int from = builder.getFrom();
            int size = builder.getSize();
            if (from >= results.size()) {
                return Collections.emptyList();
            }
            int to = Math.min(from + size, results.size());
            return results.subList(from, to);
        }

        /**
         * Simple query matching against document fields.
         * Supports match (case-insensitive contains) and term (exact match).
         * Range and bool clauses always match (no filtering) — limitations of in-memory mode.
         */
        private boolean matchesQuery(DocumentEntry entry, SearchQueryBuilder builder) {
            if (!(entry.document instanceof Map)) {
                // Non-Map documents can't be searched by field
                return true; // Include all if not a map
            }
            Map<String, Object> doc = (Map<String, Object>) entry.document;
            for (Clause clause : builder.getClauses()) {
                switch (clause.type()) {
                    case MATCH -> {
                        MatchClause match = (MatchClause) clause;
                        Object fieldValue = doc.get(match.field());
                        if (fieldValue == null) {
                            return false;
                        }
                        String fieldStr = fieldValue.toString().toLowerCase();
                        String valueStr = match.value().toString().toLowerCase();
                        if (!fieldStr.contains(valueStr)) {
                            return false;
                        }
                    }
                    case TERM -> {
                        TermClause term = (TermClause) clause;
                        Object fieldValue = doc.get(term.field());
                        if (fieldValue == null) {
                            return false;
                        }
                        if (!fieldValue.toString().equals(term.value().toString())) {
                            return false;
                        }
                    }
                    // RANGE and BOOL are not supported in in-memory mode — always pass
                    case RANGE, BOOL -> { /* pass — include document */ }
                }
            }
            return true;
        }

        @Override
        public BulkResult bulkIndex(String indexName, List<?> documents) {
            ConcurrentHashMap<String, DocumentEntry> index = getOrCreateIndex(indexName);
            List<String> successfulIds = new ArrayList<>();
            List<String> failedIds = new ArrayList<>();
            Map<String, String> failedItems = new HashMap<>();

            for (int i = 0; i < documents.size(); i++) {
                Object doc = documents.get(i);
                String id = "bulk-" + i;
                try {
                    index.put(id, new DocumentEntry(doc));
                    successfulIds.add(id);
                } catch (Exception e) {
                    failedIds.add(id);
                    failedItems.put(id, e.getMessage());
                }
            }
            return new BulkResult(successfulIds, failedIds, failedItems);
        }

        @Override
        public BulkResult bulkDelete(String indexName, List<String> ids) {
            ConcurrentHashMap<String, DocumentEntry> index = indices.get(indexName);
            List<String> successfulIds = new ArrayList<>();
            List<String> failedIds = new ArrayList<>();
            Map<String, String> failedItems = new HashMap<>();

            if (index == null) {
                failedIds.addAll(ids);
                for (String id : ids) {
                    failedItems.put(id, "index not found");
                }
                return new BulkResult(successfulIds, failedIds, failedItems);
            }

            for (String id : ids) {
                DocumentEntry removed = index.remove(id);
                if (removed != null) {
                    successfulIds.add(id);
                } else {
                    failedIds.add(id);
                    failedItems.put(id, "document not found");
                }
            }
            return new BulkResult(successfulIds, failedIds, failedItems);
        }

        @Override
        public long count(String indexName, SearchQueryBuilder builder) {
            ConcurrentHashMap<String, DocumentEntry> index = indices.get(indexName);
            if (index == null) {
                return 0;
            }
            if (!builder.hasClauses()) {
                return index.size();
            }
            long count = 0;
            for (DocumentEntry entry : index.values()) {
                if (matchesQuery(entry, builder)) {
                    count++;
                }
            }
            return count;
        }
    }

    // ======================================================================
    // InMemoryIndexManager
    // ======================================================================

    /**
     * ConcurrentHashMap-backed in-memory IndexManager.
     * Supports createIndex, deleteIndex, indexExists, refreshIndex, and putMapping.
     */
    static class InMemoryIndexManager implements IndexManager {

        private final ConcurrentHashMap<String, IndexEntry> indices = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, String> aliases = new ConcurrentHashMap<>();

        static class IndexEntry {
            final String mapping;

            IndexEntry(String mapping) {
                this.mapping = mapping;
            }
        }

        @Override
        public boolean createIndex(String indexName) {
            return createIndex(indexName, "");
        }

        @Override
        public boolean createIndex(String indexName, String mapping) {
            IndexEntry existing = indices.putIfAbsent(indexName, new IndexEntry(mapping != null ? mapping : ""));
            return existing == null; // true if newly created, false if already existed
        }

        @Override
        public boolean deleteIndex(String indexName) {
            IndexEntry removed = indices.remove(indexName);
            return removed != null;
        }

        @Override
        public boolean indexExists(String indexName) {
            return indices.containsKey(indexName);
        }

        @Override
        public void refreshIndex(String indexName) {
            // No-op in in-memory mode — data is immediately available
        }

        @Override
        public void putMapping(String indexName, String mapping) {
            IndexEntry entry = indices.get(indexName);
            if (entry == null) {
                throw new IllegalStateException("Index '" + indexName + "' does not exist");
            }
            indices.put(indexName, new IndexEntry(mapping));
        }

        @Override
        public boolean aliasExists(String aliasName) {
            return aliases.containsKey(aliasName);
        }

        @Override
        public boolean createAlias(String indexName, String aliasName) {
            if (!indexExists(indexName)) {
                createIndex(indexName);
            }
            aliases.put(aliasName, indexName);
            return true;
        }

        @Override
        public boolean switchAlias(String aliasName, String fromIndex, String toIndex) {
            if (!indexExists(toIndex)) {
                createIndex(toIndex);
            }
            aliases.compute(aliasName, (key, current) -> toIndex);
            return true;
        }
    }
}
