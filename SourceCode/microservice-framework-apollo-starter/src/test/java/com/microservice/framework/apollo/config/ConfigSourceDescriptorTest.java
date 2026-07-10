package com.microservice.framework.apollo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        @DisplayName("apollo() 应创建 APOLLO 类型描述符")
        void apolloShouldCreateApolloDescriptor() {
            ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.apollo(
                    "my-app", "default", List.of("application", "application.yml"), 1000L);

            assertThat(descriptor.getSourceType()).isEqualTo(ConfigSourceDescriptor.SourceType.APOLLO);
            assertThat(descriptor.getDataId()).isEqualTo("my-app");
            assertThat(descriptor.getGroup()).isEqualTo("default");
            assertThat(descriptor.getNamespace()).isEqualTo("application,application.yml");
            assertThat(descriptor.getTimestamp()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("apollo() 使用 null namespaces 时 namespace 应为 null")
        void apolloWithNullNamespacesShouldHaveNullNamespace() {
            ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.apollo(
                    "my-app", "default", null, 1000L);

            assertThat(descriptor.getSourceType()).isEqualTo(ConfigSourceDescriptor.SourceType.APOLLO);
            assertThat(descriptor.getNamespace()).isNull();
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
            ConfigSourceDescriptor d1 = ConfigSourceDescriptor.apollo(
                    "my-app", "default", List.of("application"), 1000L);
            ConfigSourceDescriptor d2 = ConfigSourceDescriptor.apollo(
                    "my-app", "default", List.of("application"), 1000L);

            assertThat(d1).isEqualTo(d2);
            assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
        }

        @Test
        @DisplayName("不同类型或参数的描述符应不相等")
        void descriptorsWithDifferentParametersShouldNotBeEqual() {
            ConfigSourceDescriptor d1 = ConfigSourceDescriptor.apollo(
                    "my-app", "default", List.of("application"), 1000L);
            ConfigSourceDescriptor d2 = ConfigSourceDescriptor.apollo(
                    "my-app", "prod-cluster", List.of("application"), 1000L);

            assertThat(d1).isNotEqualTo(d2);
        }

        @Test
        @DisplayName("不同类型的描述符应不相等")
        void descriptorsWithDifferentTypesShouldNotBeEqual() {
            ConfigSourceDescriptor d1 = ConfigSourceDescriptor.apollo(
                    "my-app", "default", List.of("application"), 1000L);
            ConfigSourceDescriptor d2 = ConfigSourceDescriptor.kubernetes(
                    "my-app", "default", 1000L);

            assertThat(d1).isNotEqualTo(d2);
        }
    }

    @Test
    @DisplayName("toString 应包含所有字段信息")
    void toStringShouldContainAllFields() {
        ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.apollo(
                "my-app", "default", List.of("application"), 1000L);

        String str = descriptor.toString();
        assertThat(str).contains("APOLLO");
        assertThat(str).contains("my-app");
        assertThat(str).contains("default");
        assertThat(str).contains("application");
        assertThat(str).contains("1000");
    }

    @Test
    @DisplayName("dataId 可为 null（某些配置源无需 dataId）")
    void dataIdCanBeNull() {
        ConfigSourceDescriptor descriptor = ConfigSourceDescriptor.apollo(null, "default", List.of("application"), 1000L);
        assertThat(descriptor.getDataId()).isNull();
        assertThat(descriptor.getSourceType()).isEqualTo(ConfigSourceDescriptor.SourceType.APOLLO);
    }
}
