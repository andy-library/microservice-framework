package com.microservice.framework.database.autoconfigure;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;
import com.microservice.framework.database.DatabaseProperties;
import com.microservice.framework.database.api.OutboxPublisher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 事务性 Outbox 自动配置。
 * <p>
 * 默认关闭，应用显式设置 {@code framework.database.outbox.enabled=true} 后激活。
 */
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
@EnableConfigurationProperties(DatabaseProperties.class)
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnBean(JdbcTemplate.class)
@ConditionalOnProperty(prefix = "framework.database.outbox", name = "enabled", havingValue = "true")
public class OutboxAutoConfiguration {

    private static final String MODULE = "DATABASE";
    private static final String SQL_IDENTIFIER_PATTERN = "[A-Za-z][A-Za-z0-9_]{0,63}";

    @Bean
    @ConditionalOnMissingBean(OutboxPublisher.class)
    public OutboxPublisher jdbcOutboxPublisher(JdbcTemplate jdbcTemplate, DatabaseProperties properties) {
        return new JdbcOutboxPublisher(jdbcTemplate, properties.getOutbox());
    }

    static class JdbcOutboxPublisher implements OutboxPublisher, InitializingBean {

        private final JdbcTemplate jdbcTemplate;
        private final DatabaseProperties.OutboxProperties properties;

        JdbcOutboxPublisher(JdbcTemplate jdbcTemplate, DatabaseProperties.OutboxProperties properties) {
            this.jdbcTemplate = jdbcTemplate;
            this.properties = properties;
        }

        @Override
        public void afterPropertiesSet() {
            if (properties.isAutoCreateTable()) {
                jdbcTemplate.execute("""
                        create table if not exists %s (
                            event_id varchar(64) primary key,
                            aggregate_type varchar(128) not null,
                            aggregate_id varchar(128) not null,
                            event_type varchar(128) not null,
                            payload text not null,
                            published boolean not null default false,
                            created_at timestamp not null
                        )
                        """.formatted(tableName()));
            }
        }

        @Override
        public void publish(String aggregateType, String aggregateId, String eventType, String payload) {
            requireNonBlank(aggregateType, "aggregateType");
            requireNonBlank(aggregateId, "aggregateId");
            requireNonBlank(eventType, "eventType");
            requireNonBlank(payload, "payload");
            jdbcTemplate.update("""
                            insert into %s
                            (event_id, aggregate_type, aggregate_id, event_type, payload, published, created_at)
                            values (?, ?, ?, ?, ?, ?, ?)
                            """.formatted(tableName()),
                    UUID.randomUUID().toString(), aggregateType, aggregateId, eventType, payload, false,
                    Timestamp.from(Instant.now()));
        }

        @Override
        public void markPublished(String eventId) {
            requireNonBlank(eventId, "eventId");
            int updated = jdbcTemplate.update(
                    "update %s set published = ? where event_id = ?".formatted(tableName()), true, eventId);
            if (updated != 1) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "OUTBOX", 1),
                        "Outbox event not found or not uniquely updated: " + eventId);
            }
        }

        @Override
        public List<OutboxEvent> findUnpublished() {
            return jdbcTemplate.queryForList("""
                            select event_id, aggregate_type, aggregate_id, event_type, payload, published, created_at
                            from %s
                            where published = ?
                            order by created_at asc
                            limit ?
                            """.formatted(tableName()), false, properties.getMaxFetchSize())
                    .stream()
                    .map(this::toEvent)
                    .toList();
        }

        private OutboxEvent toEvent(Map<String, Object> row) {
            return new OutboxEvent(
                    value(row, "event_id"),
                    value(row, "aggregate_type"),
                    value(row, "aggregate_id"),
                    value(row, "event_type"),
                    value(row, "payload"),
                    Boolean.TRUE.equals(row.get("published")),
                    toInstant(row.get("created_at")));
        }

        private String value(Map<String, Object> row, String key) {
            Object value = row.get(key);
            if (value == null) {
                value = row.get(key.toUpperCase(java.util.Locale.ROOT));
            }
            return value != null ? value.toString() : null;
        }

        private Instant toInstant(Object value) {
            if (value instanceof Timestamp timestamp) {
                return timestamp.toInstant();
            }
            if (value instanceof LocalDateTime localDateTime) {
                return localDateTime.toInstant(ZoneOffset.UTC);
            }
            if (value instanceof Instant instant) {
                return instant;
            }
            return Instant.parse(value.toString());
        }

        private String tableName() {
            String tableName = properties.getTableName();
            if (tableName == null || !tableName.matches(SQL_IDENTIFIER_PATTERN)) {
                throw new IllegalArgumentException("Invalid outbox table name: " + tableName);
            }
            return tableName;
        }

        private void requireNonBlank(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
        }
    }
}
