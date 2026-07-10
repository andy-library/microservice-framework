package com.microservice.framework.nacos.autoconfigure;

import com.microservice.framework.nacos.ConfigGovernanceProperties;
import com.microservice.framework.nacos.NacosProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.Locale;

/**
 * Nacos starter production safety checks.
 */
@AutoConfiguration
@EnableConfigurationProperties({NacosProperties.class, ConfigGovernanceProperties.class})
@ConditionalOnProperty(prefix = "framework.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NacosProductionSafetyAutoConfiguration {

    private static final long PROD_MAX_TIMEOUT_MS = 5000L;

    @Bean
    SmartInitializingSingleton nacosProductionSafetyValidator(NacosProperties nacosProperties,
                                                              ConfigGovernanceProperties governanceProperties,
                                                              Environment environment) {
        return () -> {
            if (!environment.acceptsProfiles(Profiles.of("prod")) || !nacosProperties.isEnabled()) {
                return;
            }
            validateNacos(nacosProperties);
            validateGovernance(governanceProperties);
        };
    }

    private static void validateNacos(NacosProperties properties) {
        String serverAddr = properties.getServerAddr();
        if (serverAddr != null) {
            String normalized = serverAddr.toLowerCase(Locale.ROOT);
            if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
                throw new IllegalStateException(
                        "local Nacos server-addr is not allowed in prod profile");
            }
        }
        if (properties.getTimeout() > PROD_MAX_TIMEOUT_MS) {
            throw new IllegalStateException(
                    "framework.nacos.timeout must not exceed 5000ms in prod profile");
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
