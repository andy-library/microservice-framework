package com.microservice.framework.nacos.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridges framework Nacos properties to Spring Boot ConfigData before remote config loading begins.
 */
public class NacosConfigDataEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "frameworkNacosConfigData";
    static final String NACOS_DEFAULT_LOGGING_ENABLED = "nacos.logging.default.config.enabled";

    private static final String NACOS_IMPORT_PREFIX = "optional:nacos:";
    private static final String KUBERNETES_IMPORT = "optional:kubernetes:";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("framework.nacos.enabled", Boolean.class, false)) {
            if (System.getProperty(NACOS_DEFAULT_LOGGING_ENABLED) == null) {
                System.setProperty(NACOS_DEFAULT_LOGGING_ENABLED, "false");
            }
            Map<String, Object> disabledProperties = new LinkedHashMap<>();
            disabledProperties.put("spring.cloud.nacos.config.enabled", "false");
            disabledProperties.put("spring.cloud.nacos.config.import-check.enabled", "false");
            environment.getPropertySources().addFirst(
                    new MapPropertySource(PROPERTY_SOURCE_NAME, disabledProperties));
            return;
        }

        String serverAddr = requiredText(environment, "framework.nacos.server-addr", "localhost:8848");
        String group = requiredText(environment, "framework.nacos.group", "DEFAULT_GROUP");
        String dataId = resolveDataId(environment);
        String namespace = environment.getProperty("framework.nacos.namespace", "");
        boolean kubernetesEnabled = environment.getProperty(
                "framework.nacos.kubernetes.enabled", Boolean.class, false);

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> imports = new ArrayList<>();
        if (kubernetesEnabled) {
            imports.add(KUBERNETES_IMPORT);
            putIfHasText(properties, "spring.cloud.kubernetes.config.name",
                    environment.getProperty("framework.nacos.kubernetes.config-map-name"));
            putIfHasText(properties, "spring.cloud.kubernetes.config.namespace",
                    environment.getProperty("framework.nacos.kubernetes.namespace"));
        }

        imports.add(NACOS_IMPORT_PREFIX + dataId + "?group=" + group + "&refreshEnabled=true&preference=remote");

        properties.put("spring.config.import", mergeImports(imports, environment.getProperty("spring.config.import")));
        properties.put("spring.cloud.nacos.config.enabled", "true");
        properties.put("spring.cloud.nacos.config.server-addr", serverAddr);
        properties.put("spring.cloud.nacos.config.group", group);
        properties.put("spring.cloud.nacos.config.refresh-enabled", "true");
        properties.put("spring.cloud.nacos.config.timeout", environment.getProperty("framework.nacos.timeout", "3000"));
        properties.put("spring.cloud.nacos.config.max-retry",
                environment.getProperty("framework.nacos.max-retry", "3"));
        putIfHasText(properties, "spring.cloud.nacos.config.namespace", namespace);

        properties.put("framework.config.precedence", "remote,kubernetes,application");
        properties.put("framework.config.active-sources", activeSources(kubernetesEnabled));
        properties.put("framework.config.source-details", sourceDetails(dataId, group, kubernetesEnabled));

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    private static String resolveDataId(ConfigurableEnvironment environment) {
        String configuredDataId = environment.getProperty("framework.nacos.data-id");
        if (StringUtils.hasText(configuredDataId)) {
            return configuredDataId.trim();
        }
        String applicationName = environment.getProperty("spring.application.name");
        if (StringUtils.hasText(applicationName)) {
            return applicationName.trim() + ".yml";
        }
        throw new IllegalStateException(
                "framework.nacos.data-id or spring.application.name must not be blank when framework.nacos.enabled=true");
    }

    private static String requiredText(ConfigurableEnvironment environment, String key, String defaultValue) {
        String value = environment.getProperty(key, defaultValue);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(key + " must not be blank when framework.nacos.enabled=true");
        }
        return value.trim();
    }

    private static String mergeImports(List<String> imports, String existingImport) {
        String generatedImports = String.join(",", imports);
        if (!StringUtils.hasText(existingImport)) {
            return generatedImports;
        }
        return generatedImports + "," + existingImport.trim();
    }

    private static List<String> activeSources(boolean kubernetesEnabled) {
        List<String> sources = new ArrayList<>();
        sources.add("nacos");
        if (kubernetesEnabled) {
            sources.add("kubernetes");
        }
        sources.add("application");
        return sources;
    }

    private static Map<String, Object> sourceDetails(String dataId, String group, boolean kubernetesEnabled) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("remote", Map.of("type", "nacos", "dataId", dataId, "group", group));
        if (kubernetesEnabled) {
            details.put("kubernetes", Map.of("type", "kubernetes"));
        }
        details.put("application", Map.of("type", "spring-boot"));
        return details;
    }

    private static void putIfHasText(Map<String, Object> properties, String key, String value) {
        if (StringUtils.hasText(value)) {
            properties.put(key, value.trim());
        }
    }
}
