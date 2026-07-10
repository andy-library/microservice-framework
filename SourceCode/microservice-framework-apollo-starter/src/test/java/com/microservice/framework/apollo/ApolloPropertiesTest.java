package com.microservice.framework.apollo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApolloProperties 测试
 * <p>
 * 验证 Apollo 配置属性的默认值、属性绑定和验证逻辑。
 *
 * @author Andy Yang
 */
class ApolloPropertiesTest {

    @Nested
    @DisplayName("默认值")
    class DefaultValuesTests {

        @Test
        @DisplayName("默认 enabled 应为 true")
        void defaultEnabledShouldBeTrue() {
            ApolloProperties properties = new ApolloProperties();
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认 appId 应为 null")
        void defaultAppIdShouldBeNull() {
            ApolloProperties properties = new ApolloProperties();
            assertThat(properties.getAppId()).isNull();
        }

        @Test
        @DisplayName("默认 cluster 应为 'default'")
        void defaultClusterShouldBeDefault() {
            ApolloProperties properties = new ApolloProperties();
            assertThat(properties.getCluster()).isEqualTo("default");
        }

        @Test
        @DisplayName("默认 namespaces 应为 ['application']")
        void defaultNamespacesShouldBeApplication() {
            ApolloProperties properties = new ApolloProperties();
            assertThat(properties.getNamespaces()).containsExactly("application");
        }

        @Test
        @DisplayName("默认 env 应为 null")
        void defaultEnvShouldBeNull() {
            ApolloProperties properties = new ApolloProperties();
            assertThat(properties.getEnv()).isNull();
        }

        @Test
        @DisplayName("默认 metaServerUrl 应为 null")
        void defaultMetaServerUrlShouldBeNull() {
            ApolloProperties properties = new ApolloProperties();
            assertThat(properties.getMetaServerUrl()).isNull();
        }

        @Test
        @DisplayName("默认 Kubernetes 配置应为不启用")
        void defaultKubernetesShouldNotBeEnabled() {
            ApolloProperties properties = new ApolloProperties();
            assertThat(properties.getKubernetes().isEnabled()).isFalse();
            assertThat(properties.getKubernetes().getConfigMapName()).isNull();
            assertThat(properties.getKubernetes().getNamespace()).isNull();
        }
    }

    @Nested
    @DisplayName("属性绑定")
    class PropertyBindingTests {

        @Test
        @DisplayName("设置 appId 应正确绑定")
        void settingAppIdShouldBeBound() {
            ApolloProperties properties = new ApolloProperties();
            properties.setAppId("my-service");
            assertThat(properties.getAppId()).isEqualTo("my-service");
        }

        @Test
        @DisplayName("设置 cluster 应正确绑定")
        void settingClusterShouldBeBound() {
            ApolloProperties properties = new ApolloProperties();
            properties.setCluster("prod-cluster");
            assertThat(properties.getCluster()).isEqualTo("prod-cluster");
        }

        @Test
        @DisplayName("设置 namespaces 应正确绑定")
        void settingNamespacesShouldBeBound() {
            ApolloProperties properties = new ApolloProperties();
            List<String> namespaces = new ArrayList<>(List.of("application", "application.yml", "db.properties"));
            properties.setNamespaces(namespaces);
            assertThat(properties.getNamespaces()).containsExactly("application", "application.yml", "db.properties");
        }

        @Test
        @DisplayName("设置 env 应正确绑定")
        void settingEnvShouldBeBound() {
            ApolloProperties properties = new ApolloProperties();
            properties.setEnv("PRO");
            assertThat(properties.getEnv()).isEqualTo("PRO");
        }

        @Test
        @DisplayName("设置 metaServerUrl 应正确绑定")
        void settingMetaServerUrlShouldBeBound() {
            ApolloProperties properties = new ApolloProperties();
            properties.setMetaServerUrl("http://meta.apollo:8080");
            assertThat(properties.getMetaServerUrl()).isEqualTo("http://meta.apollo:8080");
        }

        @Test
        @DisplayName("禁用 enabled 应正确绑定")
        void disablingEnabledShouldBeBound() {
            ApolloProperties properties = new ApolloProperties();
            properties.setEnabled(false);
            assertThat(properties.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("验证逻辑")
    class ValidationTests {

        @Test
        @DisplayName("appId 为 null 时不应视为有效")
        void nullAppIdShouldNotBeValid() {
            ApolloProperties properties = new ApolloProperties();
            assertThat(properties.getAppId()).isNull();
        }

        @Test
        @DisplayName("appId 为空字符串时不应视为有效")
        void blankAppIdShouldNotBeValid() {
            ApolloProperties properties = new ApolloProperties();
            properties.setAppId("");
            assertThat(properties.getAppId()).isBlank();
        }

        @Test
        @DisplayName("appId 为纯空格时不应视为有效")
        void whitespaceAppIdShouldNotBeValid() {
            ApolloProperties properties = new ApolloProperties();
            properties.setAppId("   ");
            assertThat(properties.getAppId()).isBlank();
        }

        @Test
        @DisplayName("appId 有实际值时应视为有效")
        void nonBlankAppIdShouldBeValid() {
            ApolloProperties properties = new ApolloProperties();
            properties.setAppId("my-app");
            assertThat(properties.getAppId()).isNotBlank();
        }
    }
}
