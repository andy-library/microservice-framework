package com.microservice.framework.audit.autoconfigure;

import com.microservice.framework.audit.AuditProperties;
import com.microservice.framework.audit.api.AuditRecorder;
import com.microservice.framework.audit.api.AuditTamperEvidenceKeyProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
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
            assertThat(context).hasSingleBean(AuditTamperEvidenceKeyProvider.class);
        });
    }

    @Test
    @DisplayName("B 端强制审计时禁止关闭 Audit Starter")
    void mandatoryBSideAuditShouldRejectDisablingAudit() {
        contextRunner.withPropertyValues("framework.audit.enabled=false")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("mandatory prod audit must fail closed when no tamper evidence key is available")
    void mandatoryProdAuditShouldRequireTamperEvidenceKey() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class,
                        AuditAutoConfiguration.class))
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "spring.datasource.url=jdbc:h2:mem:audit-prod-no-key;MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "framework.audit.storage.type=JDBC")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("mandatory prod audit should start when key provider bean is supplied")
    void mandatoryProdAuditShouldAcceptExternalKeyProvider() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class,
                        AuditAutoConfiguration.class))
                .withBean(AuditTamperEvidenceKeyProvider.class, () -> () -> "provider-secret".getBytes())
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "spring.datasource.url=jdbc:h2:mem:audit-prod-provider-key;MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "framework.audit.storage.type=JDBC")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AuditTamperEvidenceKeyProvider.class);
                });
    }

    @Test
    @DisplayName("非 B 端应用显式取消强制审计后允许关闭 Audit Starter")
    void nonMandatoryApplicationMayDisableAudit() {
        contextRunner.withPropertyValues(
                        "framework.audit.enabled=false",
                        "framework.audit.bside.mandatory=false")
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
                "framework.audit.security.checksum-algorithm=HmacSHA512",
                "framework.audit.security.tamper-evidence-key=external-secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AuditProperties properties = context.getBean(AuditProperties.class);
                    assertThat(properties.getSecurity().getChecksumEnabled()).isFalse();
                    assertThat(properties.getSecurity().getChecksumAlgorithm()).isEqualTo("HmacSHA512");
                    assertThat(properties.getSecurity().getTamperEvidenceKey()).isEqualTo("external-secret");
                });
    }

    @Test
    @DisplayName("async audit configuration must fail because async outbox is not implemented")
    void asyncAuditConfigurationShouldFailUntilAsyncOutboxExists() {
        contextRunner.withPropertyValues("framework.audit.storage.async=true")
                .run(context -> assertThat(context).hasFailed());
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
            assertThat(retrieved.get().getChecksum()).isNotEqualTo(entry.getChecksum());
            assertThat(retrieved.get().verifyChecksum("HmacSHA256", "dev-test-audit-key".getBytes())).isTrue();
        });
    }
}
