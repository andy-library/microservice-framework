package com.microservice.framework.apollo.autoconfigure;

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
 * Bridges framework Apollo properties to Spring Boot ConfigData before remote config loading begins.
 */
public class ApolloConfigDataEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "frameworkApolloConfigData";

    private static final String APOLLO_IMPORT_PREFIX = "optional:apollo://";
    private static final String KUBERNETES_IMPORT = "optional:kubernetes:";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("framework.apollo.enabled", Boolean.class, false)) {
            return;
        }

        String appId = requiredText(environment, "framework.apollo.app-id", null);
        String cluster = requiredText(environment, "framework.apollo.cluster", "default");
        String metaServerUrl = requiredText(environment, "framework.apollo.meta-server-url", null);
        List<String> namespaces = resolveNamespaces(environment);
        boolean kubernetesEnabled = environment.getProperty(
                "framework.apollo.kubernetes.enabled", Boolean.class, false);

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> imports = new ArrayList<>();
        if (kubernetesEnabled) {
            imports.add(KUBERNETES_IMPORT);
            putIfHasText(properties, "spring.cloud.kubernetes.config.name",
                    environment.getProperty("framework.apollo.kubernetes.config-map-name"));
            putIfHasText(properties, "spring.cloud.kubernetes.config.namespace",
                    environment.getProperty("framework.apollo.kubernetes.namespace"));
        }
        namespaces.forEach(namespace -> imports.add(APOLLO_IMPORT_PREFIX + namespace));

        properties.put("spring.config.import", mergeImports(imports, environment.getProperty("spring.config.import")));
        properties.put("app.id", appId);
        properties.put("apollo.cluster", cluster);
        properties.put("apollo.meta", metaServerUrl);
        putIfHasText(properties, "env", environment.getProperty("framework.apollo.env"));

        properties.put("framework.config.precedence", "remote,kubernetes,application");
        properties.put("framework.config.active-sources", activeSources(kubernetesEnabled));
        properties.put("framework.config.source-details", sourceDetails(namespaces, kubernetesEnabled));

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    private static List<String> resolveNamespaces(ConfigurableEnvironment environment) {
        List<String> namespaces = new ArrayList<>();
        for (int index = 0; ; index++) {
            String indexedNamespace = environment.getProperty("framework.apollo.namespaces[" + index + "]");
            if (indexedNamespace == null) {
                break;
            }
            if (StringUtils.hasText(indexedNamespace)) {
                namespaces.add(indexedNamespace.trim());
            }
        }

        if (namespaces.isEmpty()) {
            String commaSeparatedNamespaces = environment.getProperty("framework.apollo.namespaces");
            if (StringUtils.hasText(commaSeparatedNamespaces)) {
                for (String namespace : commaSeparatedNamespaces.split(",")) {
                    if (StringUtils.hasText(namespace)) {
                        namespaces.add(namespace.trim());
                    }
                }
            }
        }

        if (namespaces.isEmpty()) {
            namespaces.add("application");
        }
        return List.copyOf(namespaces);
    }

    private static String requiredText(ConfigurableEnvironment environment, String key, String defaultValue) {
        String value = environment.getProperty(key, defaultValue);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(key + " must not be blank when framework.apollo.enabled=true");
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
        sources.add("apollo");
        if (kubernetesEnabled) {
            sources.add("kubernetes");
        }
        sources.add("application");
        return sources;
    }

    private static Map<String, Object> sourceDetails(List<String> namespaces, boolean kubernetesEnabled) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("remote", Map.of("type", "apollo", "namespaces", String.join(",", namespaces)));
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
