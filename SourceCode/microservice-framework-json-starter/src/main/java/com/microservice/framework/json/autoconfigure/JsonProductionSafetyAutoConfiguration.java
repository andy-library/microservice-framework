package com.microservice.framework.json.autoconfigure;

import com.microservice.framework.json.JsonProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * JSON starter production safety checks.
 */
@AutoConfiguration
@EnableConfigurationProperties(JsonProperties.class)
public class JsonProductionSafetyAutoConfiguration {

    private static final int PROD_MAX_DEPTH_LIMIT = 200;
    private static final int PROD_MAX_PAYLOAD_SIZE_LIMIT = 2 * 1024 * 1024;

    @Bean
    SmartInitializingSingleton jsonProductionSafetyValidator(JsonProperties properties,
                                                             Environment environment) {
        return () -> {
            if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                return;
            }
            validateMaxDepth(properties);
            validateMaxPayloadSize(properties);
        };
    }

    private static void validateMaxDepth(JsonProperties properties) {
        if (properties.getMaxDepth() > PROD_MAX_DEPTH_LIMIT) {
            throw new IllegalStateException(
                    "framework.json.max-depth must not exceed 200 in prod profile");
        }
    }

    private static void validateMaxPayloadSize(JsonProperties properties) {
        int maxPayloadSize = properties.getMaxPayloadSize();
        if (maxPayloadSize == 0) {
            throw new IllegalStateException(
                    "framework.json.max-payload-size must be enabled in prod profile");
        }
        if (maxPayloadSize > PROD_MAX_PAYLOAD_SIZE_LIMIT) {
            throw new IllegalStateException(
                    "framework.json.max-payload-size must not exceed 2097152 in prod profile");
        }
    }
}
