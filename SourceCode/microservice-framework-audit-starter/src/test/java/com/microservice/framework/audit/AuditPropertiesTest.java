package com.microservice.framework.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuditProperties 绑定和默认值测试
 * <p>
 * 验证各嵌套配置组的默认值符合设计规格，
 * 并验证属性绑定后值能正确覆盖默认值。
 *
 * @author Andy Yang
 */
class AuditPropertiesTest {

    @Test
    @DisplayName("默认存储配置应与设计规格一致")
    void defaultStoragePropertiesShouldMatchSpecification() {
        AuditProperties properties = new AuditProperties();
        AuditProperties.StorageProperties storage = properties.getStorage();

        assertThat(storage.getType()).isEqualTo("MEMORY");
        assertThat(storage.getAsync()).isTrue();
    }

    @Test
    @DisplayName("默认留存配置应与设计规格一致")
    void defaultRetentionPropertiesShouldMatchSpecification() {
        AuditProperties properties = new AuditProperties();
        AuditProperties.RetentionProperties retention = properties.getRetention();

        assertThat(retention.getDays()).isEqualTo(365);
    }

    @Test
    @DisplayName("默认安全配置应与设计规格一致")
    void defaultSecurityPropertiesShouldMatchSpecification() {
        AuditProperties properties = new AuditProperties();
        AuditProperties.SecurityProperties security = properties.getSecurity();

        assertThat(security.getChecksumEnabled()).isTrue();
        assertThat(security.getChecksumAlgorithm()).isEqualTo("SHA-256");
    }

    @Test
    @DisplayName("默认 B 端配置应与设计规格一致")
    void defaultBSidePropertiesShouldMatchSpecification() {
        AuditProperties properties = new AuditProperties();
        AuditProperties.BSideProperties bside = properties.getBside();

        assertThat(bside.getMandatory()).isTrue();
    }

    @Test
    @DisplayName("自定义属性值应能覆盖默认值")
    void customPropertyValuesShouldOverrideDefaults() {
        AuditProperties properties = new AuditProperties();

        properties.getStorage().setType("DATABASE");
        properties.getStorage().setAsync(false);
        assertThat(properties.getStorage().getType()).isEqualTo("DATABASE");
        assertThat(properties.getStorage().getAsync()).isFalse();

        properties.getRetention().setDays(180);
        assertThat(properties.getRetention().getDays()).isEqualTo(180);

        properties.getSecurity().setChecksumEnabled(false);
        properties.getSecurity().setChecksumAlgorithm("SHA-512");
        assertThat(properties.getSecurity().getChecksumEnabled()).isFalse();
        assertThat(properties.getSecurity().getChecksumAlgorithm()).isEqualTo("SHA-512");

        properties.getBside().setMandatory(false);
        assertThat(properties.getBside().getMandatory()).isFalse();
    }
}
