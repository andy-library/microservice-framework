package com.microservice.framework.observability.autoconfigure.tracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bridges framework baggage keys into Spring Boot's tracing baggage settings.
 */
public class BaggagePropagationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "frameworkObservabilityBaggagePropagation";
    private static final String FRAMEWORK_BAGGAGE_KEYS = "framework.observability.tracing.baggage-keys";
    private static final String BOOT_REMOTE_FIELDS = "management.tracing.baggage.remote-fields";
    private static final String BOOT_CORRELATION_FIELDS = "management.tracing.baggage.correlation.fields";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> baggageKeys = Binder.get(environment)
                .bind(FRAMEWORK_BAGGAGE_KEYS, Bindable.listOf(String.class))
                .orElse(List.of())
                .stream()
                .filter(BaggagePropagationEnvironmentPostProcessor::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (baggageKeys.isEmpty()) {
            return;
        }

        String joinedKeys = String.join(",", baggageKeys);
        Map<String, Object> bridgedProperties = new LinkedHashMap<>();
        addDefaultIfMissing(environment, bridgedProperties, BOOT_REMOTE_FIELDS, joinedKeys);
        addDefaultIfMissing(environment, bridgedProperties, BOOT_CORRELATION_FIELDS, joinedKeys);
        if (!bridgedProperties.isEmpty()) {
            environment.getPropertySources().addLast(
                    new MapPropertySource(PROPERTY_SOURCE_NAME, bridgedProperties));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static void addDefaultIfMissing(Environment environment, Map<String, Object> target,
                                            String key, String value) {
        if (!environment.containsProperty(key)) {
            target.put(key, value);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
