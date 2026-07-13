package com.microservice.framework.apollo.autoconfigure;

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

class ApolloConfigDataIntegrationTest {

    private final ApolloConfigDataEnvironmentPostProcessor postProcessor =
            new ApolloConfigDataEnvironmentPostProcessor();

    @Test
    @DisplayName("enabled Apollo starter puts official client and ConfigData resolver on runtime classpath")
    void enabledStarterShouldHaveOfficialClientAndConfigDataResolverOnClasspath() {
        ClassLoader classLoader = getClass().getClassLoader();

        assertThat(ClassUtils.isPresent("com.ctrip.framework.apollo.Config", classLoader)).isTrue();
        assertThat(ClassUtils.isPresent(
                "com.ctrip.framework.apollo.config.data.importer.ApolloConfigDataLocationResolver", classLoader))
                .isTrue();
    }

    @Test
    @DisplayName("enabled Apollo starter contributes executable ConfigData imports")
    void enabledStarterShouldContributeConfigDataImports() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of(
                "framework.apollo.enabled=true",
                "framework.apollo.app-id=orders",
                "framework.apollo.cluster=prod",
                "framework.apollo.namespaces[0]=application",
                "framework.apollo.namespaces[1]=business",
                "framework.apollo.meta-server-url=http://apollo-meta:8080")
                .applyTo(environment);

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.config.import"))
                .isEqualTo("optional:apollo://application,optional:apollo://business");
        assertThat(environment.getProperty("app.id")).isEqualTo("orders");
        assertThat(environment.getProperty("apollo.cluster")).isEqualTo("prod");
        assertThat(environment.getProperty("apollo.meta")).isEqualTo("http://apollo-meta:8080");
    }

    @Test
    @DisplayName("enabled Apollo starter validates required identity before ConfigData loading")
    void enabledStarterShouldValidateRequiredIdentity() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of(
                "framework.apollo.enabled=true",
                "framework.apollo.app-id= ",
                "framework.apollo.meta-server-url=http://apollo-meta:8080")
                .applyTo(environment);

        assertThatIllegalStateException()
                .isThrownBy(() -> postProcessor.postProcessEnvironment(environment, new SpringApplication()))
                .withMessageContaining("framework.apollo.app-id must not be blank");
    }

    @Test
    @DisplayName("disabled Apollo starter does not contribute ConfigData import")
    void disabledStarterShouldNotContributeConfigDataImport() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of(
                "framework.apollo.enabled=false",
                "spring.config.import=optional:file:./application-local.yml")
                .applyTo(environment);

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.config.import"))
                .isEqualTo("optional:file:./application-local.yml");
        assertThat(environment.getPropertySources().contains("frameworkApolloConfigData")).isFalse();
    }

    @Test
    @DisplayName("Apollo and Kubernetes imports have executable remote-first precedence diagnostics")
    void kubernetesAndRemotePrecedenceShouldBeExecutableAndDiagnosable() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of(
                "framework.apollo.enabled=true",
                "framework.apollo.app-id=orders",
                "framework.apollo.cluster=prod",
                "framework.apollo.namespaces[0]=application",
                "framework.apollo.namespaces[1]=business",
                "framework.apollo.meta-server-url=http://apollo-meta:8080",
                "framework.apollo.kubernetes.enabled=true")
                .applyTo(environment);

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.config.import"))
                .isEqualTo("optional:kubernetes:,optional:apollo://application,optional:apollo://business");
        assertThat(environment.getProperty("framework.config.precedence"))
                .isEqualTo("remote,kubernetes,application");

        MapPropertySource diagnostics =
                (MapPropertySource) environment.getPropertySources().get("frameworkApolloConfigData");
        assertThat(diagnostics.getProperty("framework.config.active-sources"))
                .isEqualTo(List.of("apollo", "kubernetes", "application"));

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceDetails =
                (Map<String, Object>) diagnostics.getProperty("framework.config.source-details");
        assertThat(sourceDetails.get("remote"))
                .isEqualTo(Map.of("type", "apollo", "namespaces", "application,business"));
    }
}
