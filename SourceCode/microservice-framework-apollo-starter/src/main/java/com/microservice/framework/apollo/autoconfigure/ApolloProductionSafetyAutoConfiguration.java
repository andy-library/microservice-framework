package com.microservice.framework.apollo.autoconfigure;

import com.microservice.framework.apollo.ApolloProperties;
import com.microservice.framework.apollo.ConfigGovernanceProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.Locale;

/**
 * Apollo starter production safety checks.
 */
@AutoConfiguration
@EnableConfigurationProperties({ApolloProperties.class, ConfigGovernanceProperties.class})
@ConditionalOnProperty(prefix = "framework.apollo", name = "enabled", havingValue = "true")
public class ApolloProductionSafetyAutoConfiguration {

    @Bean
    SmartInitializingSingleton apolloProductionSafetyValidator(ApolloProperties apolloProperties,
                                                               ConfigGovernanceProperties governanceProperties,
                                                               Environment environment) {
        return () -> {
            if (!environment.acceptsProfiles(Profiles.of("prod")) || !apolloProperties.isEnabled()) {
                return;
            }
            validateApollo(apolloProperties);
            validateGovernance(governanceProperties);
        };
    }

    private static void validateApollo(ApolloProperties properties) {
        if (properties.getAppId() == null || properties.getAppId().isBlank()) {
            throw new IllegalStateException(
                    "framework.apollo.app-id must be configured in prod profile");
        }
        String metaServerUrl = properties.getMetaServerUrl();
        if (metaServerUrl == null || metaServerUrl.isBlank()) {
            throw new IllegalStateException(
                    "framework.apollo.meta-server-url must be configured in prod profile");
        }
        String normalized = metaServerUrl.toLowerCase(Locale.ROOT);
        if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
            throw new IllegalStateException(
                    "local Apollo meta-server-url is not allowed in prod profile");
        }
    }

    private static void validateGovernance(ConfigGovernanceProperties properties) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException(
                    "framework.config.enabled cannot be disabled in prod profile");
        }
        if (!properties.getValidator().isEnabled()) {
            throw new IllegalStateException(
                    "framework.config.validator.enabled cannot be disabled in prod profile");
        }
        if (!properties.getRefreshPolicy().isEnabled()) {
            throw new IllegalStateException(
                    "framework.config.refresh-policy.enabled cannot be disabled in prod profile");
        }
        if (!properties.getMasking().isEnabled()) {
            throw new IllegalStateException(
                    "framework.config.masking.enabled cannot be disabled in prod profile");
        }
    }
}
