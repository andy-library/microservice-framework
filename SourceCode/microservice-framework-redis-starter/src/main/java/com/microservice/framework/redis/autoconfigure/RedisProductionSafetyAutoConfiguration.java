package com.microservice.framework.redis.autoconfigure;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;
import com.microservice.framework.redis.RedisProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Redis production safety auto-configuration.
 * <p>
 * Performs fail-fast checks for settings that may overload Redis or weaken the
 * framework cache contract in production.
 */
@AutoConfiguration
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnProperty(prefix = "framework.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisProductionSafetyAutoConfiguration {

    private static final String MODULE = "REDIS";
    private static final long PROD_MAX_SCAN_BATCH_SIZE = 1_000L;
    private static final long PROD_MAX_NULL_VALUE_TTL_SECONDS = 300L;
    private static final long PROD_MAX_LOCK_TIMEOUT_MS = 5_000L;
    private static final long PROD_MAX_LOCK_EXPIRE_MS = 60_000L;
    private static final long PROD_MAX_RATE_LIMIT_PERIOD_SECONDS = 60L;

    /**
     * Creates a production safety validator.
     *
     * @param properties Redis properties
     * @param environment Spring environment
     * @return startup validator
     */
    @Bean
    public SmartInitializingSingleton redisProductionSafetyValidator(
            RedisProperties properties,
            Environment environment) {
        return () -> {
            if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                return;
            }
            validateCache(properties.getCache());
            validateLock(properties.getLock());
            validateRateLimit(properties.getRateLimit());
        };
    }

    private static void validateCache(RedisProperties.CacheProperties cache) {
        if (!Boolean.TRUE.equals(cache.getEnabled())) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 1),
                    "framework.redis.cache.enabled cannot be disabled in prod profile");
        }
        if (cache.getScanBatchSize() > PROD_MAX_SCAN_BATCH_SIZE) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 2),
                    "framework.redis.cache.scan-batch-size must not exceed 1000 in prod profile");
        }
        if (cache.getNullValueTtl() > PROD_MAX_NULL_VALUE_TTL_SECONDS) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 3),
                    "framework.redis.cache.null-value-ttl must not exceed 300s in prod profile");
        }
    }

    private static void validateLock(RedisProperties.LockProperties lock) {
        if (lock.getDefaultTimeout() > PROD_MAX_LOCK_TIMEOUT_MS) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 4),
                    "framework.redis.lock.default-timeout must not exceed 5000ms in prod profile");
        }
        if (lock.getDefaultExpire() > PROD_MAX_LOCK_EXPIRE_MS) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 5),
                    "framework.redis.lock.default-expire must not exceed 60000ms in prod profile");
        }
    }

    private static void validateRateLimit(RedisProperties.RateLimitProperties rateLimit) {
        if (rateLimit.getDefaultPeriod() > PROD_MAX_RATE_LIMIT_PERIOD_SECONDS) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 6),
                    "framework.redis.rate-limit.default-period must not exceed 60s in prod profile");
        }
    }
}
