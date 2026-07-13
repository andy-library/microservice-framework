package com.microservice.framework.audit.autoconfigure;

import com.microservice.framework.audit.AuditProperties;
import com.microservice.framework.audit.api.AuditEntry;
import com.microservice.framework.audit.api.AuditQuery;
import com.microservice.framework.audit.api.AuditRecorder;
import com.microservice.framework.audit.api.AuditTamperEvidenceKeyProvider;
import com.microservice.framework.common.page.PageRequest;
import com.microservice.framework.common.page.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC 审计记录器。
 *
 * @author Andy Yang
 */
final class JdbcAuditRecorder implements AuditRecorder {

    private static final String TABLE_NAME_PATTERN = "[A-Za-z_][A-Za-z0-9_]*";

    private final JdbcTemplate jdbcTemplate;
    private final AuditProperties properties;
    private final AuditTamperEvidenceKeyProvider keyProvider;
    private final TransactionTemplate transactionTemplate;
    private final String tableName;

    JdbcAuditRecorder(JdbcTemplate jdbcTemplate, AuditProperties properties) {
        this(jdbcTemplate, properties, () -> "dev-test-audit-key".getBytes(java.nio.charset.StandardCharsets.UTF_8), null);
    }

    JdbcAuditRecorder(JdbcTemplate jdbcTemplate, AuditProperties properties,
                      AuditTamperEvidenceKeyProvider keyProvider,
                      PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.keyProvider = Objects.requireNonNull(keyProvider, "keyProvider must not be null");
        this.transactionTemplate = transactionManager == null ? null : new TransactionTemplate(transactionManager);
        this.tableName = validateTableName(properties.getStorage().getTableName());
        if (Boolean.TRUE.equals(properties.getStorage().getAutoCreateTable())) {
            createTable();
        }
    }

    @Override
    public void record(AuditEntry entry) {
        AuditEntry normalized = normalize(entry);
        String sql = "insert into " + tableName
                + " (id, event_type, operator_id, target_id, action, detail, event_timestamp, checksum)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                normalized.getId(),
                normalized.getEventType(),
                normalized.getOperatorId().orElse(null),
                normalized.getTargetId().orElse(null),
                normalized.getAction(),
                normalized.getDetail().orElse(null),
                Timestamp.from(normalized.getTimestamp()),
                normalized.getChecksum());
    }

    @Override
    public void recordBatch(List<AuditEntry> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        if (entries.isEmpty()) {
            return;
        }
        if (transactionTemplate == null) {
            throw new IllegalStateException("JDBC audit recordBatch requires a transaction manager to avoid partial writes");
        }
        transactionTemplate.executeWithoutResult(status -> entries.forEach(this::record));
    }

    @Override
    public Optional<AuditEntry> getEntry(String id) {
        String sql = "select id, event_type, operator_id, target_id, action, detail, event_timestamp, checksum"
                + " from " + tableName + " where id = ?";
        List<AuditEntry> entries = jdbcTemplate.query(sql, this::mapEntry, id);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        AuditEntry entry = entries.get(0);
        verifyChecksum(entry);
        return Optional.of(entry);
    }

    @Override
    public PageResult<AuditEntry> query(AuditQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        StringBuilder whereClause = new StringBuilder(" where 1 = 1");
        List<Object> args = new ArrayList<>();
        query.getStartTime().ifPresent(start -> {
            whereClause.append(" and event_timestamp >= ?");
            args.add(Timestamp.from(start));
        });
        query.getEndTime().ifPresent(end -> {
            whereClause.append(" and event_timestamp <= ?");
            args.add(Timestamp.from(end));
        });
        query.getOperatorId().ifPresent(operatorId -> {
            whereClause.append(" and operator_id = ?");
            args.add(operatorId);
        });
        query.getEventType().ifPresent(eventType -> {
            whereClause.append(" and event_type = ?");
            args.add(eventType);
        });
        query.getAction().ifPresent(action -> {
            whereClause.append(" and action = ?");
            args.add(action);
        });
        PageRequest page = query.getPageRequest();
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from " + tableName + whereClause, Long.class, args.toArray());
        long totalCount = total == null ? 0L : total;
        int from = (page.getPageNumber() - 1) * page.getPageSize();
        if (from >= totalCount) {
            return PageResult.of(totalCount, List.of(), page.getPageNumber(), page.getPageSize());
        }
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(page.getPageSize());
        pageArgs.add(from);
        String sql = "select id, event_type, operator_id, target_id, action, detail, event_timestamp, checksum from "
                + tableName + whereClause + " order by event_timestamp desc, id desc limit ? offset ?";
        List<AuditEntry> entries = jdbcTemplate.query(sql, this::mapEntry, pageArgs.toArray());
        entries.forEach(this::verifyChecksum);
        return PageResult.of(totalCount, entries, page.getPageNumber(), page.getPageSize());
    }

    private AuditEntry normalize(AuditEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        Instant timestamp = entry.getTimestamp().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        return new AuditEntry(entry.getId(), entry.getEventType(), entry.getOperatorId().orElse(null),
                entry.getTargetId().orElse(null), entry.getAction(), entry.getDetail().orElse(null), timestamp,
                properties.getSecurity().getChecksumAlgorithm(), keyProvider.currentKey());
    }

    private AuditEntry mapEntry(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        Timestamp timestamp = resultSet.getTimestamp("event_timestamp");
        return AuditEntry.restoreFromStorage(
                resultSet.getString("id"),
                resultSet.getString("event_type"),
                resultSet.getString("operator_id"),
                resultSet.getString("target_id"),
                resultSet.getString("action"),
                resultSet.getString("detail"),
                timestamp.toInstant(),
                resultSet.getString("checksum"));
    }

    private void verifyChecksum(AuditEntry entry) {
        if (Boolean.TRUE.equals(properties.getSecurity().getChecksumEnabled())
                && !entry.verifyChecksum(properties.getSecurity().getChecksumAlgorithm(), keyProvider.currentKey())) {
            throw new IllegalStateException("Audit entry checksum verification failed for id: " + entry.getId());
        }
    }

    private void createTable() {
        jdbcTemplate.execute("create table if not exists " + tableName + " ("
                + "id varchar(255) primary key,"
                + "event_type varchar(255) not null,"
                + "operator_id varchar(255),"
                + "target_id varchar(255),"
                + "action varchar(255) not null,"
                + "detail text,"
                + "event_timestamp timestamp(6) not null,"
                + "checksum varchar(255) not null"
                + ")");
    }

    private static String validateTableName(String value) {
        if (value == null || !value.matches(TABLE_NAME_PATTERN)) {
            throw new IllegalArgumentException("Audit table name must contain only letters, digits, and underscores");
        }
        return value;
    }
}
