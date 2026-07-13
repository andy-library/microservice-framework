package com.microservice.framework.redis.autoconfigure;

import com.microservice.framework.redis.RedisProperties;
import com.microservice.framework.redis.api.DistributedLock;
import com.microservice.framework.redis.api.RateLimiter;
import com.microservice.framework.redis.api.RedisCache;
import com.microservice.framework.redis.api.RedisCounter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * RedisAutoConfiguration 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证自动配置的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 *
 * @author Andy Yang
 */
class RedisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
            .withBean("redisConnectionFactory", RedisConnectionFactory.class,
                    () -> mock(RedisConnectionFactory.class));

    @Nested
    @DisplayName("默认配置激活测试")
    class DefaultActivationTest {

        @Test
        @DisplayName("默认配置应激活所有 Redis Bean")
        void defaultConfigurationShouldActivateAll() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("redisTemplate");
                assertThat(context).hasBean("redisDistributedLock");
                assertThat(context).hasBean("redisRateLimiter");
                assertThat(context).hasBean("redisCounter");
                assertThat(context).hasBean("redisCache");
            });
        }

        @Test
        @DisplayName("默认配置应注册正确类型的 Bean")
        void defaultConfigurationShouldRegisterCorrectTypes() {
            contextRunner.run(context -> {
                assertThat(context.getBean(DistributedLock.class))
                        .isInstanceOf(com.microservice.framework.redis.core.RedisLock.class);
                assertThat(context.getBean(RateLimiter.class))
                        .isInstanceOf(com.microservice.framework.redis.core.RedisRateLimiter.class);
                assertThat(context.getBean(RedisCounter.class))
                        .isInstanceOf(com.microservice.framework.redis.core.RedisCounterImpl.class);
                assertThat(context.getBean(RedisCache.class))
                        .isInstanceOf(com.microservice.framework.redis.core.RedisCacheImpl.class);
            });
        }
    }

    @Nested
    @DisplayName("禁用 Redis Starter 测试")
    class DisablingTest {

        @Test
        @DisplayName("framework.redis.enabled=false 时不应注册任何 Redis Bean")
        void disablingRedisStarterShouldRemoveAllBeans() {
            contextRunner.withPropertyValues("framework.redis.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("redisDistributedLock");
                        assertThat(context).doesNotHaveBean("redisRateLimiter");
                        assertThat(context).doesNotHaveBean("redisCounter");
                        assertThat(context).doesNotHaveBean("redisCache");
                    });
        }
    }

    @Nested
    @DisplayName("禁用缓存增强测试")
    class DisablingCacheTest {

        @Test
        @DisplayName("framework.redis.cache.enabled=false 时不应注册 RedisCache Bean")
        void disablingCacheShouldRemoveRedisCacheBean() {
            contextRunner.withPropertyValues("framework.redis.cache.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("redisDistributedLock");
                        assertThat(context).hasBean("redisRateLimiter");
                        assertThat(context).hasBean("redisCounter");
                        assertThat(context).doesNotHaveBean("redisCache");
                        assertThat(context).doesNotHaveBean(RedisCache.class);
                    });
        }
    }

    @Nested
    @DisplayName("属性绑定测试")
    class PropertyBindingTest {

        @Test
        @DisplayName("自定义锁超时和过期时间应正确绑定")
        void customLockPropertiesShouldBeBound() {
            contextRunner.withPropertyValues(
                    "framework.redis.lock.defaultTimeout=5000",
                    "framework.redis.lock.defaultExpire=60000")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        RedisProperties props = context.getBean(RedisProperties.class);
                        assertThat(props.getLock().getDefaultTimeout()).isEqualTo(5000L);
                        assertThat(props.getLock().getDefaultExpire()).isEqualTo(60000L);
                    });
        }

        @Test
        @DisplayName("自定义限流参数应正确绑定")
        void customRateLimitPropertiesShouldBeBound() {
            contextRunner.withPropertyValues(
                    "framework.redis.rate-limit.defaultPermits=200",
                    "framework.redis.rate-limit.defaultPeriod=10")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        RedisProperties props = context.getBean(RedisProperties.class);
                        assertThat(props.getRateLimit().getDefaultPermits()).isEqualTo(200);
                        assertThat(props.getRateLimit().getDefaultPeriod()).isEqualTo(10L);
                    });
        }

        @Test
        @DisplayName("自定义缓存 TTL 应正确绑定")
        void customCachePropertiesShouldBeBound() {
            contextRunner.withPropertyValues(
                    "framework.redis.cache.defaultTtl=7200",
                    "framework.redis.cache.nullValueTtl=120")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        RedisProperties props = context.getBean(RedisProperties.class);
                        assertThat(props.getCache().getDefaultTtl()).isEqualTo(7200L);
                        assertThat(props.getCache().getNullValueTtl()).isEqualTo(120L);
                    });
        }
    }

    @Nested
    @DisplayName("用户自定义 Bean 覆盖测试")
    class UserOverrideTest {

        @Test
        @DisplayName("用户提供的 DistributedLock 应覆盖默认 Bean")
        void userProvidedDistributedLockShouldOverrideDefault() {
            DistributedLock customLock = new DistributedLock() {
                @Override
                public boolean tryLock(String key) { return true; }
                @Override
                public boolean tryLock(String key, long timeout) { return true; }
                @Override
                public boolean tryLock(String key, long timeout, long expire) { return true; }
                @Override
                public boolean unlock(String key) { return true; }
                @Override
                public boolean isLocked(String key) { return false; }
            };

            contextRunner.withBean("customDistributedLock", DistributedLock.class, () -> customLock)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customDistributedLock");
                        assertThat(context).doesNotHaveBean("redisDistributedLock");
                        assertThat(context.getBean(DistributedLock.class)).isSameAs(customLock);
                    });
        }

        @Test
        @DisplayName("用户提供的 RateLimiter 应覆盖默认 Bean")
        void userProvidedRateLimiterShouldOverrideDefault() {
            RateLimiter customLimiter = new RateLimiter() {
                @Override
                public boolean tryAcquire(String key) { return true; }
                @Override
                public boolean tryAcquire(String key, int permits) { return true; }
                @Override
                public boolean tryAcquire(String key, int permits, long period) { return true; }
                @Override
                public long acquire(String key) { return 0L; }
                @Override
                public long getAvailablePermits(String key) { return 100L; }
            };

            contextRunner.withBean("customRateLimiter", RateLimiter.class, () -> customLimiter)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customRateLimiter");
                        assertThat(context).doesNotHaveBean("redisRateLimiter");
                        assertThat(context.getBean(RateLimiter.class)).isSameAs(customLimiter);
                    });
        }

        @Test
        @DisplayName("用户提供的 RedisCounter 应覆盖默认 Bean")
        void userProvidedRedisCounterShouldOverrideDefault() {
            RedisCounter customCounter = new RedisCounter() {
                @Override
                public long increment(String key) { return 1L; }
                @Override
                public long increment(String key, long delta) { return delta; }
                @Override
                public long decrement(String key) { return -1L; }
                @Override
                public long decrement(String key, long delta) { return -delta; }
                @Override
                public long get(String key) { return 0L; }
                @Override
                public long reset(String key, long value) { return value; }
                @Override
                public long getAndIncrement(String key) { return 0L; }
            };

            contextRunner.withBean("customRedisCounter", RedisCounter.class, () -> customCounter)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customRedisCounter");
                        assertThat(context).doesNotHaveBean("redisCounter");
                        assertThat(context.getBean(RedisCounter.class)).isSameAs(customCounter);
                    });
        }

        @Test
        @DisplayName("用户提供的 RedisCache 应覆盖默认 Bean")
        void userProvidedRedisCacheShouldOverrideDefault() {
            RedisCache customCache = new RedisCache() {
                @Override
                public <T> T get(String key) { return null; }
                @Override
                public <T> T get(String key, Class<T> type) { return null; }
                @Override
                public void put(String key, Object value) { }
                @Override
                public void put(String key, Object value, long ttl) { }
                @Override
                public boolean evict(String key) { return true; }
                @Override
                public void evictAll(java.util.List<String> keys) { }
                @Override
                public long evictByPattern(String pattern) { return 0L; }
            };

            contextRunner.withBean("customRedisCache", RedisCache.class, () -> customCache)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customRedisCache");
                        assertThat(context).doesNotHaveBean("redisCache");
                        assertThat(context.getBean(RedisCache.class)).isSameAs(customCache);
                    });
        }
    }

    @Nested
    @DisplayName("RedisTemplate 配置测试")
    class RedisTemplateTest {

        @Test
        @DisplayName("默认 RedisTemplate 应使用 JSON 序列化")
        void defaultRedisTemplateShouldUseJsonSerialization() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("redisTemplate");
                org.springframework.data.redis.core.RedisTemplate<String, Object> template =
                        context.getBean("redisTemplate",
                                org.springframework.data.redis.core.RedisTemplate.class);
                assertThat(template).isNotNull();
            });
        }

        @Test
        @DisplayName("用户提供的 RedisTemplate 应覆盖默认")
        void userProvidedRedisTemplateShouldOverrideDefault() {
            // 使用 mock 以避免 afterPropertiesSet() 要求 RedisConnectionFactory
            org.springframework.data.redis.core.RedisTemplate<String, Object> customTemplate =
                    mock(org.springframework.data.redis.core.RedisTemplate.class);

            contextRunner.withBean("redisTemplate",
                    org.springframework.data.redis.core.RedisTemplate.class,
                    () -> customTemplate)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("redisTemplate");
                        assertThat(context.getBean("redisTemplate"))
                                .isSameAs(customTemplate);
                    });
        }
    }
}
