package com.microservice.framework.audit.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AuditEntry 测试
 * <p>
 * 验证 checksum 计算和校验、不可变性以及基本属性。
 *
 * @author Andy Yang
 */
class AuditEntryTest {

    @Test
    @DisplayName("AuditEntry 创建时应自动计算 SHA-256 checksum")
    void auditEntryCreationShouldComputeChecksum() {
        Instant now = Instant.now();
        AuditEntry entry = new AuditEntry(
                "id-1", "LOGIN", "user-001", "target-001",
                "login", "user logged in", now, "SHA-256");

        assertThat(entry.getChecksum()).isNotNull();
        assertThat(entry.getChecksum()).isNotEmpty();
        // SHA-256 produces 64 hex chars
        assertThat(entry.getChecksum()).hasSize(64);
    }

    @Test
    @DisplayName("verifyChecksum 应正确校验未被篡改的条目")
    void verifyChecksumShouldPassForUntamperedEntry() {
        Instant now = Instant.now();
        AuditEntry entry = new AuditEntry(
                "id-1", "LOGIN", "user-001", "target-001",
                "login", "user logged in", now, "SHA-256");

        assertThat(entry.verifyChecksum("SHA-256")).isTrue();
    }

    @Test
    @DisplayName("相同字段值应产生相同 checksum")
    void sameFieldValuesShouldProduceSameChecksum() {
        Instant now = Instant.now();
        AuditEntry entry1 = new AuditEntry(
                "id-1", "LOGIN", "user-001", "target-001",
                "login", "user logged in", now, "SHA-256");
        AuditEntry entry2 = new AuditEntry(
                "id-2", "LOGIN", "user-001", "target-001",
                "login", "user logged in", now, "SHA-256");

        // checksum is based on content fields, not id
        assertThat(entry1.getChecksum()).isEqualTo(entry2.getChecksum());
    }

    @Test
    @DisplayName("不同字段值应产生不同 checksum")
    void differentFieldValuesShouldProduceDifferentChecksum() {
        Instant now = Instant.now();
        AuditEntry entry1 = new AuditEntry(
                "id-1", "LOGIN", "user-001", "target-001",
                "login", "description 1", now, "SHA-256");
        AuditEntry entry2 = new AuditEntry(
                "id-2", "LOGIN", "user-001", "target-001",
                "login", "description 2", now, "SHA-256");

        assertThat(entry1.getChecksum()).isNotEqualTo(entry2.getChecksum());
    }

    @Test
    @DisplayName("null 字段应正确参与 checksum 计算")
    void nullFieldsShouldBeHandledInChecksum() {
        Instant now = Instant.now();
        AuditEntry entryWithNulls = new AuditEntry(
                "id-1", "LOGIN", null, null,
                "login", null, now, "SHA-256");

        assertThat(entryWithNulls.verifyChecksum("SHA-256")).isTrue();
    }

    @Test
    @DisplayName("AuditEntry 使用预计算 checksum 创建时应正确存储")
    void auditEntryWithPrecomputedChecksumShouldStoreCorrectly() {
        Instant now = Instant.now();
        AuditEntry original = new AuditEntry(
                "id-1", "LOGIN", "user-001", "target-001",
                "login", "description", now, "SHA-256");

        // Re-create with precomputed checksum (from storage recovery)
        AuditEntry restored = AuditEntry.restoreFromStorage(
                "id-1", "LOGIN", "user-001", "target-001",
                "login", "description", now, original.getChecksum());

        assertThat(restored.getChecksum()).isEqualTo(original.getChecksum());
        assertThat(restored.verifyChecksum("SHA-256")).isTrue();
    }

    @Test
    @DisplayName("必填字段为 null 时应抛出 NullPointerException")
    void requiredNullFieldsShouldThrowNullPointerException() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> new AuditEntry(null, "LOGIN", "op", "tgt", "act", "desc", now, "SHA-256"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditEntry("id", null, "op", "tgt", "act", "desc", now, "SHA-256"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditEntry("id", "LOGIN", "op", "tgt", null, "desc", now, "SHA-256"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditEntry("id", "LOGIN", "op", "tgt", "act", "desc", null, "SHA-256"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("不支持的算法应抛出 IllegalArgumentException")
    void unsupportedAlgorithmShouldThrowIllegalArgumentException() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new AuditEntry(
                "id-1", "LOGIN", "op", "tgt", "act", "desc", now, "INVALID-ALGO"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Optional getter 应正确返回值")
    void optionalGettersShouldReturnCorrectValues() {
        Instant now = Instant.now();
        AuditEntry entry = new AuditEntry(
                "id-1", "LOGIN", "user-001", "target-001",
                "login", "desc", now, "SHA-256");

        assertThat(entry.getOperatorId()).contains("user-001");
        assertThat(entry.getTargetId()).contains("target-001");
        assertThat(entry.getDetail()).contains("desc");

        AuditEntry entryWithNulls = new AuditEntry(
                "id-2", "LOGOUT", null, null,
                "logout", null, now, "SHA-256");

        assertThat(entryWithNulls.getOperatorId()).isEmpty();
        assertThat(entryWithNulls.getTargetId()).isEmpty();
        assertThat(entryWithNulls.getDetail()).isEmpty();
    }
}
