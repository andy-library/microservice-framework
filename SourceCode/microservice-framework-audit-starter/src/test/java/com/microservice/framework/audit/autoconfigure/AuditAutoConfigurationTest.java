package com.microservice.framework.audit.autoconfigure;

import com.microservice.framework.audit.AuditProperties;
import com.microservice.framework.audit.api.AuditRecorder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit Starter 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证自动配置的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 *
 * @author Andy Yang
 */
class AuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

    @Test
    @DisplayName("默认配置应激活 Audit 自动配置")
    void defaultConfigurationShouldActivateAudit() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("auditRecorder");
            assertThat(context.getBean(AuditRecorder.class)).isNotNull();
        });
    }

    @Test
    @DisplayName("禁用 Audit Starter 后 AuditRecorder Bean 不应存在")
    void disablingAuditShouldRemoveAuditRecorder() {
        contextRunner.withPropertyValues("framework.audit.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AuditRecorder.class);
                });
    }

    @Test
    @DisplayName("自定义存储和留存配置应正确绑定")
    void customStorageAndRetentionPropertiesShouldBindCorrectly() {
        contextRunner.withPropertyValues(
                "framework.audit.storage.type=MEMORY",
                "framework.audit.storage.async=false",
                "framework.audit.retention.days=180")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AuditProperties properties = context.getBean(AuditProperties.class);
                    assertThat(properties.getStorage().getType()).isEqualTo("MEMORY");
                    assertThat(properties.getStorage().getAsync()).isFalse();
                    assertThat(properties.getRetention().getDays()).isEqualTo(180);
                });
    }

    @Test
    @DisplayName("自定义安全配置应正确绑定")
    void customSecurityPropertiesShouldBindCorrectly() {
        contextRunner.withPropertyValues(
                "framework.audit.security.checksum-enabled=false",
                "framework.audit.security.checksum-algorithm=SHA-512")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AuditProperties properties = context.getBean(AuditProperties.class);
                    assertThat(properties.getSecurity().getChecksumEnabled()).isFalse();
                    assertThat(properties.getSecurity().getChecksumAlgorithm()).isEqualTo("SHA-512");
                });
    }

    @Test
    @DisplayName("自定义 B 端配置应正确绑定")
    void customBSidePropertiesShouldBindCorrectly() {
        contextRunner.withPropertyValues(
                "framework.audit.bside.mandatory=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AuditProperties properties = context.getBean(AuditProperties.class);
                    assertThat(properties.getBside().getMandatory()).isFalse();
                });
    }

    @Test
    @DisplayName("用户提供的 AuditRecorder 应覆盖默认 Bean")
    void userProvidedAuditRecorderShouldOverrideDefault() {
        AuditRecorder customRecorder = new AuditRecorder() {
            @Override
            public void record(com.microservice.framework.audit.api.AuditEntry entry) {
            }

            @Override
            public void recordBatch(java.util.List<com.microservice.framework.audit.api.AuditEntry> entries) {
            }

            @Override
            public java.util.Optional<com.microservice.framework.audit.api.AuditEntry> getEntry(String id) {
                return java.util.Optional.empty();
            }

            @Override
            public com.microservice.framework.common.page.PageResult<com.microservice.framework.audit.api.AuditEntry> query(
                    com.microservice.framework.audit.api.AuditQuery query) {
                return com.microservice.framework.common.page.PageResult.empty(1, 20);
            }
        };

        contextRunner.withBean("customAuditRecorder", AuditRecorder.class, () -> customRecorder)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("customAuditRecorder");
                    assertThat(context).doesNotHaveBean("auditRecorder");
                });
    }

    @Test
    @DisplayName("默认 AuditRecorder 应具备基本记录能力")
    void defaultAuditRecorderShouldHaveBasicRecordingCapability() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AuditRecorder recorder = context.getBean(AuditRecorder.class);

            // Record an entry
            com.microservice.framework.audit.api.AuditEntry entry =
                    new com.microservice.framework.audit.api.AuditEntry(
                            "test-id", "TEST", "op-1", "tgt-1",
                            "action", "detail", java.time.Instant.now(), "SHA-256");
            recorder.record(entry);

            // Retrieve it
            java.util.Optional<com.microservice.framework.audit.api.AuditEntry> retrieved =
                    recorder.getEntry("test-id");
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getId()).isEqualTo("test-id");
        });
    }
}
