package com.microservice.framework.common.autoconfigure;

import com.microservice.framework.common.CommonProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Common starter production safety checks.
 */
@AutoConfiguration
@EnableConfigurationProperties(CommonProperties.class)
public class CommonProductionSafetyAutoConfiguration {

    private static final long PROD_MAX_CLOCK_BACKWARD_TOLERANCE_MS = 5000L;

    @Bean
    SmartInitializingSingleton commonProductionSafetyValidator(CommonProperties properties,
                                                               Environment environment) {
        return () -> {
            if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                return;
            }
            validateModules(properties, environment);
            validateId(properties.getId());
        };
    }

    private static void validateModules(CommonProperties properties, Environment environment) {
        if (!environment.getProperty("framework.common.time.enabled", Boolean.class, true)) {
            throw new IllegalStateException(
                    "framework.common.time.enabled cannot be disabled in prod profile");
        }
        if (!environment.getProperty("framework.common.id.enabled", Boolean.class, true)) {
            throw new IllegalStateException(
                    "framework.common.id.enabled cannot be disabled in prod profile");
        }
        if (Boolean.FALSE.equals(properties.getContext().getEnabled())) {
            throw new IllegalStateException(
                    "framework.common.context.enabled cannot be disabled in prod profile");
        }
    }

    private static void validateId(CommonProperties.IdProperties id) {
        if (id.getClockBackwardTolerance() != null
                && id.getClockBackwardTolerance() > PROD_MAX_CLOCK_BACKWARD_TOLERANCE_MS) {
            throw new IllegalStateException(
                    "framework.common.id.clock-backward-tolerance must not exceed 5000ms in prod profile");
        }
    }
}
