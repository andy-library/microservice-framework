package com.microservice.framework.audit.autoconfigure;

import com.microservice.framework.audit.api.AuditEntry;
import com.microservice.framework.audit.api.AuditQuery;
import com.microservice.framework.audit.api.AuditRecorder;
import com.microservice.framework.common.page.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcAuditRecorderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    JdbcTemplateAutoConfiguration.class,
                    AuditAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:audit-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "framework.audit.storage.type=JDBC",
                    "framework.audit.storage.auto-create-table=true",
                    "framework.audit.storage.table-name=framework_audit_event_test");

    @Test
    @DisplayName("JDBC 审计记录器应能建表写入并查询")
    void jdbcAuditRecorderShouldCreateTableRecordAndQuery() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AuditRecorder recorder = context.getBean(AuditRecorder.class);

            AuditEntry entry = new AuditEntry(
                    "audit-jdbc-1",
                    "ADMIN_ACTION",
                    "admin-001",
                    "system",
                    "UPDATE",
                    "update config",
                    Instant.parse("2026-06-18T10:00:00Z"),
                    "SHA-256");
            recorder.record(entry);

            assertThat(recorder.getEntry("audit-jdbc-1")).contains(entry);
            assertThat(recorder.query(new AuditQuery(null, null, "admin-001", "ADMIN_ACTION", null, null))
                    .getItems()).hasSize(1);
        });
    }

    @Test
    @DisplayName("JDBC 审计记录被篡改后读取应失败")
    void jdbcAuditRecorderShouldRejectTamperedEntry() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AuditRecorder recorder = context.getBean(AuditRecorder.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            AuditEntry entry = new AuditEntry(
                    "audit-jdbc-tamper",
                    "ADMIN_ACTION",
                    "admin-001",
                    "system",
                    "UPDATE",
                    "before tamper",
                    Instant.parse("2026-06-18T10:00:00Z"),
                    "SHA-256");
            recorder.record(entry);

            jdbcTemplate.update("update framework_audit_event_test set detail=? where id=?",
                    "after tamper", "audit-jdbc-tamper");

            assertThatThrownBy(() -> recorder.getEntry("audit-jdbc-tamper"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("checksum verification failed");
        });
    }

    @Test
    @DisplayName("JDBC 审计记录应按数据库微秒精度计算 checksum")
    void jdbcAuditRecorderShouldNormalizeTimestampPrecision() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AuditRecorder recorder = context.getBean(AuditRecorder.class);

            AuditEntry entry = new AuditEntry(
                    "audit-jdbc-nanos",
                    "ADMIN_ACTION",
                    "admin-001",
                    "system",
                    "UPDATE",
                    "update config with nanos",
                    Instant.parse("2026-06-18T10:00:00.123456789Z"),
                    "SHA-256");
            recorder.record(entry);

            assertThat(recorder.getEntry("audit-jdbc-nanos"))
                    .isPresent()
                    .get()
                    .satisfies(restored -> {
                        assertThat(restored.verifyChecksum("SHA-256")).isTrue();
                        assertThat(restored.getTimestamp()).isEqualTo(Instant.parse("2026-06-18T10:00:00.123456Z"));
                    });
        });
    }

    @Test
    @DisplayName("DATABASE 存储类型应兼容映射到 JDBC 审计记录器")
    void databaseStorageTypeShouldRemainCompatibleWithJdbcRecorder() {
        contextRunner.withPropertyValues("framework.audit.storage.type=DATABASE")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AuditRecorder.class);
                    assertThat(context.getBean(AuditRecorder.class)).isInstanceOf(JdbcAuditRecorder.class);
                });
    }

    @Test
    @DisplayName("查询当前页时不应因其他页被篡改的记录而失败")
    void queryShouldVerifyOnlyTheRequestedPage() {
        contextRunner.run(context -> {
            AuditRecorder recorder = context.getBean(AuditRecorder.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            recorder.record(new AuditEntry("audit-page-valid", "PAGED", "admin", "target", "READ", null,
                    Instant.parse("2026-06-18T11:00:00Z"), "SHA-256"));
            recorder.record(new AuditEntry("audit-page-tampered", "PAGED", "admin", "target", "READ", null,
                    Instant.parse("2026-06-18T10:00:00Z"), "SHA-256"));
            jdbcTemplate.update("update framework_audit_event_test set detail=? where id=?",
                    "tampered", "audit-page-tampered");

            AuditQuery firstPage = new AuditQuery(null, null, null, "PAGED", null,
                    PageRequest.builder().pageNumber(1).pageSize(1).build());

            assertThat(recorder.query(firstPage).getItems())
                    .extracting(AuditEntry::getId)
                    .containsExactly("audit-page-valid");
        });
    }

    @Test
    @DisplayName("生产环境禁止自动创建审计表")
    void productionShouldRejectAutomaticAuditTableCreation() {
        contextRunner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).hasFailed());
    }
}
