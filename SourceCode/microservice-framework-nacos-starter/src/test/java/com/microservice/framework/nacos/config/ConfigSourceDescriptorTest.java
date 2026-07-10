package com.microservice.framework.nacos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConfigSourceDescriptor 测试
 * <p>
 * 验证配置源描述符的创建、相等性判断和不可变性。
 *
 * @author Andy Yang
 */
class ConfigSourceDescriptorTest {

    @Nested
    @DisplayName("静态工厂方法")
    class FactoryMethodTests {

        @Test
        @DisplayName("nacos() 应创建 NACOS 类型描述符")
        void nacosShouldCreateNacosDescriptor() {
            ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.nacos(
                    "app.yml", "DEFAULT_GROUP", "dev", 1000L);

            assertThat(descriptor.getSourceType()).isEqualTo(ConfigSourceDescriptor.SourceType.NACOS);
            assertThat(descriptor.getDataId()).isEqualTo("app.yml");
            assertThat(descriptor.getGroup()).isEqualTo("DEFAULT_GROUP");
            assertThat(descriptor.getNamespace()).isEqualTo("dev");
            assertThat(descriptor.getTimestamp()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("kubernetes() 应创建 KUBERNETES 类型描述符")
        void kubernetesShouldCreateKubernetesDescriptor() {
            ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.kubernetes(
                    "app-config", "default", 2000L);

            assertThat(descriptor.getSourceType()).isEqualTo(ConfigSourceDescriptor.SourceType.KUBERNETES);
            assertThat(descriptor.getDataId()).isEqualTo("app-config");
            assertThat(descriptor.getNamespace()).isEqualTo("default");
            assertThat(descriptor.getTimestamp()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("environment() 应创建 ENVIRONMENT 类型描述符")
        void environmentShouldCreateEnvironmentDescriptor() {
            ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.environment(3000L);

            assertThat(descriptor.getSourceType()).isEqualTo(ConfigSourceDescriptor.SourceType.ENVIRONMENT);
            assertThat(descriptor.getDataId()).isNull();
            assertThat(descriptor.getGroup()).isNull();
            assertThat(descriptor.getNamespace()).isNull();
            assertThat(descriptor.getTimestamp()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("springBoot() 应创建 SPRING_BOOT 类型描述符")
        void springBootShouldCreateSpringBootDescriptor() {
            ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.springBoot(
                    "application.yml", 4000L);

            assertThat(descriptor.getSourceType()).isEqualTo(ConfigSourceDescriptor.SourceType.SPRING_BOOT);
            assertThat(descriptor.getDataId()).isEqualTo("application.yml");
            assertThat(descriptor.getGroup()).isNull();
            assertThat(descriptor.getTimestamp()).isEqualTo(4000L);
        }

        @Test
        @DisplayName("custom() 应创建 CUSTOM 类型描述符")
        void customShouldCreateCustomDescriptor() {
            ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.custom(
                    "vault-secret", 5000L);

            assertThat(descriptor.getSourceType()).isEqualTo(ConfigSourceDescriptor.SourceType.CUSTOM);
            assertThat(descriptor.getDataId()).isEqualTo("vault-secret");
            assertThat(descriptor.getTimestamp()).isEqualTo(5000L);
        }
    }

    @Nested
    @DisplayName("相等性判断")
    class EqualityTests {

        @Test
        @DisplayName("相同参数的描述符应相等")
        void descriptorsWithSameParametersShouldBeEqual() {
            ConfigSourceDescriptor d1 = ConfigSourceDescriptor.nacos(
                    "app.yml", "DEFAULT_GROUP", "dev", 1000L);
            ConfigSourceDescriptor d2 = ConfigSourceDescriptor.nacos(
                    "app.yml", "DEFAULT_GROUP", "dev", 1000L);

            assertThat(d1).isEqualTo(d2);
            assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
        }

        @Test
        @DisplayName("不同类型或参数的描述符应不相等")
        void descriptorsWithDifferentParametersShouldNotBeEqual() {
            ConfigSourceDescriptor d1 = ConfigSourceDescriptor.nacos(
                    "app.yml", "DEFAULT_GROUP", "dev", 1000L);
            ConfigSourceDescriptor d2 = ConfigSourceDescriptor.nacos(
                    "app.yml", "DEFAULT_GROUP", "prod", 1000L);

            assertThat(d1).isNotEqualTo(d2);
        }

        @Test
        @DisplayName("不同类型的描述符应不相等")
        void descriptorsWithDifferentTypesShouldNotBeEqual() {
            ConfigSourceDescriptor d1 = ConfigSourceDescriptor.nacos(
                    "app.yml", "DEFAULT_GROUP", "dev", 1000L);
            ConfigSourceDescriptor d2 = ConfigSourceDescriptor.kubernetes(
                    "app.yml", "dev", 1000L);

            assertThat(d1).isNotEqualTo(d2);
        }
    }

    @Test
    @DisplayName("toString 应包含所有字段信息")
    void toStringShouldContainAllFields() {
        ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.nacos(
                "app.yml", "DEFAULT_GROUP", "dev", 1000L);

        String str = descriptor.toString();
        assertThat(str).contains("NACOS");
        assertThat(str).contains("app.yml");
        assertThat(str).contains("DEFAULT_GROUP");
        assertThat(str).contains("dev");
        assertThat(str).contains("1000");
    }

    @Test
    @DisplayName("dataId 可为 null（某些配置源无需 dataId）")
    void dataIdCanBeNull() {
        ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.nacos(null, "DEFAULT_GROUP", "dev", 1000L);
        assertThat(descriptor.getDataId()).isNull();
        assertThat(descriptor.getSourceType()).isEqualTo(ConfigSourceDescriptor.SourceType.NACOS);
    }
}
