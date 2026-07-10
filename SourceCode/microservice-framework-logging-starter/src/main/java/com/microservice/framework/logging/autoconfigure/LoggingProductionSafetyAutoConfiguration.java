package com.microservice.framework.logging.autoconfigure;

import com.microservice.framework.logging.properties.LoggingProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Logging production safety auto-configuration.
 * <p>
 * Logging is a required diagnostic capability in production. This validator
 * fails fast when settings could drop logs or leak sensitive data.
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingProductionSafetyAutoConfiguration {

    /**
     * Creates a production safety validator.
     *
     * @param properties logging properties
     * @param environment Spring environment
     * @return startup validator
     */
    @Bean
    public SmartInitializingSingleton loggingProductionSafetyValidator(
            LoggingProperties properties,
            Environment environment) {
        return () -> {
            if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                return;
            }
            validateAsync(properties.getAsync());
            validateFloodProtection(properties.getFloodProtection());
            validateMasking(properties.getMasking());
        };
    }

    private static void validateAsync(LoggingProperties.AsyncProperties async) {
        if (!async.isEnabled()) {
            throw new IllegalStateException(
                    "framework.logging.async.enabled cannot be disabled in prod profile");
        }
        if (async.getDiscardingThreshold() != 0) {
            throw new IllegalStateException(
                    "framework.logging.async.discarding-threshold must be 0 in prod profile");
        }
    }

    private static void validateFloodProtection(LoggingProperties.FloodProtectionProperties floodProtection) {
        if (!floodProtection.isEnabled()) {
            throw new IllegalStateException(
                    "framework.logging.flood-protection.enabled cannot be disabled in prod profile");
        }
    }

    private static void validateMasking(LoggingProperties.MaskingProperties masking) {
        if (!masking.isEnabled()) {
            throw new IllegalStateException(
                    "framework.logging.masking.enabled cannot be disabled in prod profile");
        }
    }
}
