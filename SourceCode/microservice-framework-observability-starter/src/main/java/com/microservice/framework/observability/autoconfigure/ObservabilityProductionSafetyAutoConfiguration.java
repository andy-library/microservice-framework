package com.microservice.framework.observability.autoconfigure;

import com.microservice.framework.observability.ObservabilityProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Observability production safety auto-configuration.
 * <p>
 * The starter is optional, but once an application depends on it, production
 * deployments should not silently disable tracing or metrics.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ObservabilityProductionSafetyAutoConfiguration {

    /**
     * Creates a production safety validator.
     *
     * @param properties observability properties
     * @param environment Spring environment
     * @return startup validator
     */
    @Bean
    public SmartInitializingSingleton observabilityProductionSafetyValidator(
            ObservabilityProperties properties,
            Environment environment) {
        return () -> {
            if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                return;
            }
            validateTracing(properties.getTracing());
            validateMetrics(properties.getMetrics());
        };
    }

    private static void validateTracing(ObservabilityProperties.TracingProperties tracing) {
        if (!tracing.isEnabled()) {
            throw new IllegalStateException(
                    "framework.observability.tracing.enabled cannot be disabled in prod profile");
        }
        if (tracing.getBaggageKeys() == null || tracing.getBaggageKeys().isEmpty()) {
            throw new IllegalStateException(
                    "framework.observability.tracing.baggage-keys must not be empty in prod profile");
        }
    }

    private static void validateMetrics(ObservabilityProperties.MetricsProperties metrics) {
        if (!metrics.isEnabled()) {
            throw new IllegalStateException(
                    "framework.observability.metrics.enabled cannot be disabled in prod profile");
        }
    }
}
