package com.microservice.framework.audit.api;

import com.microservice.framework.audit.AuditProperties;
import com.microservice.framework.audit.autoconfigure.AuditAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuditRecorder 接口测试
 * <p>
 * 通过 ApplicationContextRunner 验证 AuditRecorder 接口的行为，
 * 确保默认实现满足接口契约。
 *
 * @author Andy Yang
 */
class AuditRecorderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

    @Test
    @DisplayName("AuditRecorder record 应正确记录审计条目")
    void auditRecorderRecordShouldStoreEntry() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AuditRecorder recorder = context.getBean(AuditRecorder.class);

            AuditEntry entry = new AuditEntry(
                    "id-1", "LOGIN", "user-001", "target-001",
                    "login", "user logged in", Instant.now(), "SHA-256");
            recorder.record(entry);

            Optional<AuditEntry> retrieved = recorder.getEntry("id-1");
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getId()).isEqualTo("id-1");
            assertThat(retrieved.get().getEventType()).isEqualTo("LOGIN");
        });
    }

    @Test
    @DisplayName("AuditRecorder recordBatch 应批量记录审计条目")
    void auditRecorderRecordBatchShouldStoreMultipleEntries() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AuditRecorder recorder = context.getBean(AuditRecorder.class);

            List<AuditEntry> entries = Arrays.asList(
                    new AuditEntry("id-1", "LOGIN", "user-001", "target-001",
                            "login", "login event", Instant.now(), "SHA-256"),
                    new AuditEntry("id-2", "LOGOUT", "user-001", "target-001",
                            "logout", "logout event", Instant.now(), "SHA-256"),
                    new AuditEntry("id-3", "UPDATE", "user-002", "target-002",
                            "update", "update event", Instant.now(), "SHA-256"));

            recorder.recordBatch(entries);

            assertThat(recorder.getEntry("id-1")).isPresent();
            assertThat(recorder.getEntry("id-2")).isPresent();
            assertThat(recorder.getEntry("id-3")).isPresent();
        });
    }

    @Test
    @DisplayName("AuditRecorder getEntry 应对不存在 ID 返回空 Optional")
    void auditRecorderGetEntryShouldReturnEmptyForNonexistentId() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AuditRecorder recorder = context.getBean(AuditRecorder.class);
            Optional<AuditEntry> result = recorder.getEntry("nonexistent");
            assertThat(result).isEmpty();
        });
    }

    @Test
    @DisplayName("AuditRecorder query 应返回分页结果")
    void auditRecorderQueryShouldReturnPageResult() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AuditRecorder recorder = context.getBean(AuditRecorder.class);

            recorder.record(new AuditEntry("id-1", "LOGIN", "user-001", null,
                    "login", "desc 1", Instant.now(), "SHA-256"));
            recorder.record(new AuditEntry("id-2", "LOGOUT", "user-001", null,
                    "logout", "desc 2", Instant.now(), "SHA-256"));

            AuditQuery query = AuditQuery.defaults();
            var result = recorder.query(query);
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(2);
        });
    }
}
