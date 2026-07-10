package com.microservice.framework.audit.autoconfigure;

import com.microservice.framework.audit.AuditProperties;
import com.microservice.framework.audit.api.AuditEntry;
import com.microservice.framework.audit.api.AuditQuery;
import com.microservice.framework.audit.api.AuditRecorder;
import com.microservice.framework.common.page.PageRequest;
import com.microservice.framework.common.page.PageResult;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
@ConditionalOnProperty(prefix = "framework.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
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
    public AuditRecorder auditRecorder(AuditProperties properties) {
        return new InMemoryAuditRecorder(properties);
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

        InMemoryAuditRecorder(AuditProperties properties) {
            this.properties = properties;
        }

        @Override
        public void record(AuditEntry entry) {
            store.put(entry.getId(), entry);
        }

        @Override
        public void recordBatch(List<AuditEntry> entries) {
            for (AuditEntry entry : entries) {
                store.put(entry.getId(), entry);
            }
        }

        @Override
        public Optional<AuditEntry> getEntry(String id) {
            AuditEntry entry = store.get(id);
            if (entry != null && properties.getSecurity().getChecksumEnabled()) {
                if (!entry.verifyChecksum(properties.getSecurity().getChecksumAlgorithm())) {
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
    }
}
