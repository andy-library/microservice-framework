package com.microservice.framework.audit.autoconfigure;

import com.microservice.framework.audit.AuditProperties;
import com.microservice.framework.audit.api.AuditEntry;
import com.microservice.framework.audit.api.AuditQuery;
import com.microservice.framework.audit.api.AuditRecorder;
import com.microservice.framework.audit.api.AuditTamperEvidenceKeyProvider;
import com.microservice.framework.common.page.PageRequest;
import com.microservice.framework.common.page.PageResult;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Audit Starter 自动配置
 * <p>
 * 根据 {@code framework.audit.enabled} 属性决定是否激活，默认启用。
 * 注册 {@link AuditRecorder} Bean，提供审计事件的记录和查询能力。
 * <p>
 * 当未提供自定义 AuditRecorder 时，注册基于内存的默认实现，
 * 仅用于开发和测试环境，生产环境应替换为持久化存储实现。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {

    /**
     * 注册 AuditRecorder Bean
     * <p>
     * 当容器中不存在 AuditRecorder 时，注册基于内存的默认实现。
     * 默认实现仅用于开发和测试，生产环境应配置持久化存储。
     *
     * @param properties Audit 配置属性
     * @return AuditRecorder 实例
     */
    @Bean
    @ConditionalOnMissingBean(AuditRecorder.class)
    @ConditionalOnExpression("${framework.audit.enabled:true}")
    @ConditionalOnProperty(prefix = "framework.audit.storage", name = "type", havingValue = "JDBC")
    public AuditRecorder jdbcAuditRecorder(JdbcTemplate jdbcTemplate, AuditProperties properties,
                                           AuditTamperEvidenceKeyProvider keyProvider,
                                           ObjectProvider<org.springframework.transaction.PlatformTransactionManager> transactionManager) {
        return new JdbcAuditRecorder(jdbcTemplate, properties, keyProvider, transactionManager.getIfAvailable());
    }

    /**
     * 保持历史 DATABASE 配置与 JDBC 存储实现兼容。
     */
    @Bean
    @ConditionalOnMissingBean(AuditRecorder.class)
    @ConditionalOnExpression("${framework.audit.enabled:true}")
    @ConditionalOnProperty(prefix = "framework.audit.storage", name = "type", havingValue = "DATABASE")
    public AuditRecorder databaseAuditRecorder(JdbcTemplate jdbcTemplate, AuditProperties properties,
                                               AuditTamperEvidenceKeyProvider keyProvider,
                                               ObjectProvider<org.springframework.transaction.PlatformTransactionManager> transactionManager) {
        return new JdbcAuditRecorder(jdbcTemplate, properties, keyProvider, transactionManager.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(AuditRecorder.class)
    @ConditionalOnExpression("${framework.audit.enabled:true}")
    @ConditionalOnProperty(prefix = "framework.audit.storage", name = "type", havingValue = "MEMORY", matchIfMissing = true)
    public AuditRecorder auditRecorder(AuditProperties properties, AuditTamperEvidenceKeyProvider keyProvider) {
        return new InMemoryAuditRecorder(properties, keyProvider);
    }

    @Bean
    @ConditionalOnMissingBean(AuditTamperEvidenceKeyProvider.class)
    @ConditionalOnProperty(prefix = "framework.audit.security", name = "tamper-evidence-key")
    public AuditTamperEvidenceKeyProvider configuredAuditTamperEvidenceKeyProvider(AuditProperties properties) {
        return () -> {
            String key = properties.getSecurity().getTamperEvidenceKey();
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("framework.audit.security.tamper-evidence-key must not be blank");
            }
            return key.getBytes(StandardCharsets.UTF_8);
        };
    }

    @Bean
    @Profile("!prod")
    @ConditionalOnMissingBean(AuditTamperEvidenceKeyProvider.class)
    public AuditTamperEvidenceKeyProvider developmentAuditTamperEvidenceKeyProvider() {
        return () -> "dev-test-audit-key".getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 阻止不受支持的存储类型和生产环境中的危险建表行为。
     */
    @Bean
    public SmartInitializingSingleton auditProductionSafetyValidator(
            AuditProperties properties,
            Environment environment,
            ObjectProvider<AuditTamperEvidenceKeyProvider> keyProvider) {
        return () -> {
            if (Boolean.TRUE.equals(properties.getBside().getMandatory()) && !properties.isEnabled()) {
                throw new IllegalStateException(
                        "framework.audit.enabled cannot be disabled when framework.audit.bside.mandatory=true");
            }
            String storageType = properties.getStorage().getType();
            if (!"MEMORY".equals(storageType) && !"JDBC".equals(storageType) && !"DATABASE".equals(storageType)) {
                throw new IllegalStateException("framework.audit.storage.type must be MEMORY, JDBC, or DATABASE");
            }
            if (Boolean.TRUE.equals(properties.getStorage().getAsync())) {
                throw new IllegalStateException(
                        "framework.audit.storage.async=true is not supported until a durable async outbox is implemented");
            }
            if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                return;
            }
            if (Boolean.TRUE.equals(properties.getBside().getMandatory())) {
                AuditTamperEvidenceKeyProvider provider = keyProvider.getIfAvailable();
                if (provider == null || provider.currentKey().length == 0) {
                    throw new IllegalStateException(
                            "mandatory prod audit requires an external audit tamper evidence key provider");
                }
            }
            if ("MEMORY".equals(storageType)) {
                throw new IllegalStateException("framework.audit.storage.type=MEMORY is not allowed in prod profile");
            }
            if (Boolean.TRUE.equals(properties.getStorage().getAutoCreateTable())) {
                throw new IllegalStateException(
                        "framework.audit.storage.auto-create-table cannot be enabled in prod profile");
            }
        };
    }

    // ========================================================================
    // 默认内部实现
    // ========================================================================

    /**
     * 基于 ConcurrentHashMap 的内存 AuditRecorder 实现
     * <p>
     * 仅用于开发和测试环境。生产环境应替换为数据库或文件存储实现。
     */
    static class InMemoryAuditRecorder implements AuditRecorder {

        private final ConcurrentHashMap<String, AuditEntry> store = new ConcurrentHashMap<>();
        private final AuditProperties properties;
        private final AuditTamperEvidenceKeyProvider keyProvider;

        InMemoryAuditRecorder(AuditProperties properties, AuditTamperEvidenceKeyProvider keyProvider) {
            this.properties = properties;
            this.keyProvider = keyProvider;
        }

        @Override
        public void record(AuditEntry entry) {
            AuditEntry normalized = normalize(entry);
            store.put(normalized.getId(), normalized);
        }

        @Override
        public void recordBatch(List<AuditEntry> entries) {
            for (AuditEntry entry : entries) {
                record(entry);
            }
        }

        @Override
        public Optional<AuditEntry> getEntry(String id) {
            AuditEntry entry = store.get(id);
            if (entry != null && properties.getSecurity().getChecksumEnabled()) {
                if (!entry.verifyChecksum(properties.getSecurity().getChecksumAlgorithm(), keyProvider.currentKey())) {
                    throw new IllegalStateException("Audit entry checksum verification failed for id: " + id);
                }
            }
            return Optional.ofNullable(entry);
        }

        @Override
        public PageResult<AuditEntry> query(AuditQuery query) {
            List<AuditEntry> filtered = new ArrayList<>(store.values());

            // Filter by time range
            if (query.hasTimeRange()) {
                Instant start = query.getStartTime().orElse(Instant.MIN);
                Instant end = query.getEndTime().orElse(Instant.MAX);
                filtered = filtered.stream()
                        .filter(e -> !e.getTimestamp().isBefore(start) && !e.getTimestamp().isAfter(end))
                        .toList();
            }

            // Filter by operatorId
            if (query.hasOperatorFilter()) {
                String opId = query.getOperatorId().orElse("");
                filtered = filtered.stream()
                        .filter(e -> e.getOperatorId().orElse("").equals(opId))
                        .toList();
            }

            // Filter by eventType
            if (query.hasEventTypeFilter()) {
                String type = query.getEventType().orElse("");
                filtered = filtered.stream()
                        .filter(e -> e.getEventType().equals(type))
                        .toList();
            }

            // Sort by timestamp descending
            filtered = filtered.stream()
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .toList();

            int total = filtered.size();
            PageRequest pageRequest = query.getPageRequest();
            int fromIndex = (pageRequest.getPageNumber() - 1) * pageRequest.getPageSize();
            int toIndex = Math.min(fromIndex + pageRequest.getPageSize(), total);

            if (fromIndex >= total) {
                return PageResult.empty(pageRequest.getPageNumber(), pageRequest.getPageSize());
            }

            List<AuditEntry> pageContent = filtered.subList(fromIndex, toIndex);
            return PageResult.of(total, pageContent, pageRequest.getPageNumber(), pageRequest.getPageSize());
        }

        private AuditEntry normalize(AuditEntry entry) {
            return new AuditEntry(entry.getId(), entry.getEventType(), entry.getOperatorId().orElse(null),
                    entry.getTargetId().orElse(null), entry.getAction(), entry.getDetail().orElse(null),
                    entry.getTimestamp(), properties.getSecurity().getChecksumAlgorithm(), keyProvider.currentKey());
        }
    }
}
