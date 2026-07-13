package com.microservice.framework.nacos.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class NacosConfigDataIntegrationTest {

    private final NacosConfigDataEnvironmentPostProcessor postProcessor =
            new NacosConfigDataEnvironmentPostProcessor();

    @Test
    @DisplayName("enabled Nacos starter puts official client and ConfigData resolver on runtime classpath")
    void enabledStarterShouldHaveOfficialClientAndConfigDataResolverOnClasspath() {
        ClassLoader classLoader = getClass().getClassLoader();

        assertThat(ClassUtils.isPresent("com.alibaba.nacos.api.config.ConfigService", classLoader)).isTrue();
        assertThat(ClassUtils.isPresent(
                "com.alibaba.cloud.nacos.configdata.NacosConfigDataLocationResolver", classLoader)).isTrue();
    }

    @Test
    @DisplayName("enabled Nacos starter contributes executable ConfigData import")
    void enabledStarterShouldContributeConfigDataImport() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of(
                "framework.nacos.enabled=true",
                "spring.application.name=orders",
                "framework.nacos.server-addr=nacos.prod:8848",
                "framework.nacos.group=ORDERS",
                "framework.nacos.data-id=orders.yml")
                .applyTo(environment);

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.config.import"))
                .isEqualTo("optional:nacos:orders.yml?group=ORDERS&refreshEnabled=true&preference=remote");
        assertThat(environment.getProperty("spring.cloud.nacos.config.server-addr"))
                .isEqualTo("nacos.prod:8848");
        assertThat(environment.getProperty("spring.cloud.nacos.config.group")).isEqualTo("ORDERS");
    }

    @Test
    @DisplayName("enabled Nacos starter validates required identity before ConfigData loading")
    void enabledStarterShouldValidateRequiredIdentity() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of(
                "framework.nacos.enabled=true",
                "framework.nacos.server-addr= ",
                "framework.nacos.group=DEFAULT_GROUP")
                .applyTo(environment);

        assertThatIllegalStateException()
                .isThrownBy(() -> postProcessor.postProcessEnvironment(environment, new SpringApplication()))
                .withMessageContaining("framework.nacos.server-addr must not be blank");
    }

    @Test
    @DisplayName("disabled Nacos starter does not contribute ConfigData import")
    void disabledStarterShouldNotContributeConfigDataImport() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of(
                "framework.nacos.enabled=false",
                "spring.config.import=optional:file:./application-local.yml")
                .applyTo(environment);

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.config.import"))
                .isEqualTo("optional:file:./application-local.yml");
        assertThat(environment.getProperty("spring.cloud.nacos.config.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("spring.cloud.nacos.config.import-check.enabled")).isEqualTo("false");
        assertThat(System.getProperty("nacos.logging.default.config.enabled")).isEqualTo("false");
    }

    @Test
    @DisplayName("Nacos and Kubernetes imports have executable remote-first precedence diagnostics")
    void kubernetesAndRemotePrecedenceShouldBeExecutableAndDiagnosable() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of(
                "framework.nacos.enabled=true",
                "spring.application.name=orders",
                "framework.nacos.server-addr=nacos.prod:8848",
                "framework.nacos.group=ORDERS",
                "framework.nacos.data-id=orders.yml",
                "framework.nacos.kubernetes.enabled=true")
                .applyTo(environment);

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.config.import"))
                .isEqualTo("optional:kubernetes:,optional:nacos:orders.yml?group=ORDERS&refreshEnabled=true&preference=remote");
        assertThat(environment.getProperty("framework.config.precedence"))
                .isEqualTo("remote,kubernetes,application");

        MapPropertySource diagnostics =
                (MapPropertySource) environment.getPropertySources().get("frameworkNacosConfigData");
        assertThat(diagnostics.getProperty("framework.config.active-sources"))
                .isEqualTo(List.of("nacos", "kubernetes", "application"));

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceDetails =
                (Map<String, Object>) diagnostics.getProperty("framework.config.source-details");
        assertThat(sourceDetails.get("remote"))
                .isEqualTo(Map.of("type", "nacos", "dataId", "orders.yml", "group", "ORDERS"));
    }
}
