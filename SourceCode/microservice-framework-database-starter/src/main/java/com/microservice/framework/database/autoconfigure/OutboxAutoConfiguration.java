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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
                            claimed_by varchar(128),
                            claimed_until timestamp,
                            retry_count integer not null default 0,
                            last_error varchar(1024),
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
                    """
                            update %s
                            set published = ?, claimed_by = null, claimed_until = null
                            where event_id = ?
                            """.formatted(tableName()), true, eventId);
            if (updated != 1) {
                throw new FrameworkException(
                        FrameworkErrorCode.of(MODULE, "OUTBOX", 1),
                        "Outbox event not found or not uniquely updated: " + eventId);
            }
        }

        @Override
        public List<OutboxEvent> findUnpublished() {
            return jdbcTemplate.queryForList("""
                            select event_id, aggregate_type, aggregate_id, event_type, payload, published,
                                   claimed_by, claimed_until, retry_count, last_error, created_at
                            from %s
                            where published = ?
                              and (claimed_until is null or claimed_until <= ?)
                            order by created_at asc
                            limit ?
                            """.formatted(tableName()), false, Timestamp.from(Instant.now()),
                            properties.getMaxFetchSize())
                    .stream()
                    .map(this::toEvent)
                    .toList();
        }

        @Override
        public List<OutboxEvent> claimUnpublished(String publisherId, Duration leaseDuration) {
            requireNonBlank(publisherId, "publisherId");
            if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
                throw new IllegalArgumentException("leaseDuration must be positive");
            }
            Instant now = Instant.now();
            Instant claimedUntil = now.plus(leaseDuration);
            List<OutboxEvent> claimed = new ArrayList<>();
            while (claimed.size() < properties.getMaxFetchSize()) {
                List<String> candidateIds = jdbcTemplate.queryForList("""
                                select event_id
                                from %s
                                where published = ?
                                  and (claimed_until is null or claimed_until <= ?)
                                order by created_at asc
                                limit ?
                                """.formatted(tableName()), String.class, false, Timestamp.from(now),
                        properties.getMaxFetchSize() - claimed.size());
                if (candidateIds.isEmpty()) {
                    break;
                }
                boolean claimedAtLeastOne = false;
                for (String eventId : candidateIds) {
                    int updated = jdbcTemplate.update("""
                                    update %s
                                    set claimed_by = ?,
                                        claimed_until = ?,
                                        retry_count = retry_count + 1
                                    where event_id = ?
                                      and published = ?
                                      and (claimed_until is null or claimed_until <= ?)
                                    """.formatted(tableName()),
                            publisherId, Timestamp.from(claimedUntil), eventId, false, Timestamp.from(now));
                    if (updated == 1) {
                        claimed.add(loadEvent(eventId));
                        claimedAtLeastOne = true;
                    }
                }
                if (!claimedAtLeastOne) {
                    continue;
                }
            }
            return claimed;
        }

        private OutboxEvent loadEvent(String eventId) {
            return toEvent(jdbcTemplate.queryForMap("""
                    select event_id, aggregate_type, aggregate_id, event_type, payload, published,
                           claimed_by, claimed_until, retry_count, last_error, created_at
                    from %s
                    where event_id = ?
                    """.formatted(tableName()), eventId));
        }

        private OutboxEvent toEvent(Map<String, Object> row) {
            return new OutboxEvent(
                    value(row, "event_id"),
                    value(row, "aggregate_type"),
                    value(row, "aggregate_id"),
                    value(row, "event_type"),
                    value(row, "payload"),
                    booleanValue(row, "published"),
                    toInstant(row.get("created_at")),
                    value(row, "claimed_by"),
                    nullableInstant(row.get("claimed_until")),
                    intValue(row, "retry_count"),
                    value(row, "last_error"));
        }

        private String value(Map<String, Object> row, String key) {
            Object value = row.get(key);
            if (value == null) {
                value = row.get(key.toUpperCase(java.util.Locale.ROOT));
            }
            return value != null ? value.toString() : null;
        }

        private Instant toInstant(Object value) {
            Instant instant = nullableInstant(value);
            if (instant == null) {
                throw new IllegalArgumentException("timestamp value must not be null");
            }
            return instant;
        }

        private Instant nullableInstant(Object value) {
            if (value == null) {
                return null;
            }
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

        private int intValue(Map<String, Object> row, String key) {
            Object value = row.get(key);
            if (value == null) {
                value = row.get(key.toUpperCase(java.util.Locale.ROOT));
            }
            if (value instanceof Number number) {
                return number.intValue();
            }
            return value == null ? 0 : Integer.parseInt(value.toString());
        }

        private boolean booleanValue(Map<String, Object> row, String key) {
            Object value = row.get(key);
            if (value == null) {
                value = row.get(key.toUpperCase(java.util.Locale.ROOT));
            }
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof Number number) {
                return number.intValue() != 0;
            }
            return value != null && Boolean.parseBoolean(value.toString());
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
