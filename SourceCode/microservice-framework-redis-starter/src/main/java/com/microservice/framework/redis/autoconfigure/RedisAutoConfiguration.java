package com.microservice.framework.redis.autoconfigure;

import com.microservice.framework.redis.RedisProperties;
import com.microservice.framework.redis.api.DistributedLock;
import com.microservice.framework.redis.api.RateLimiter;
import com.microservice.framework.redis.api.RedisCache;
import com.microservice.framework.redis.api.RedisCounter;
import com.microservice.framework.redis.core.RedisCacheImpl;
import com.microservice.framework.redis.core.RedisCounterImpl;
import com.microservice.framework.redis.core.RedisLock;
import com.microservice.framework.redis.core.RedisRateLimiter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Starter 自动配置
 * <p>
 * 根据 {@code framework.redis.cache.enabled} 属性决定是否激活缓存增强功能，
 * 默认启用。分布式锁、限流器、原子计数器始终激活。
 * <p>
 * 当 Spring Data Redis 在 classpath 上时，自动注册以下 Bean：
 * <ul>
 *     <li>{@link RedisTemplate} — 通用 Redis 操作模板（JSON 序列化）</li>
 *     <li>{@link StringRedisTemplate} — 字符串 Redis 操作模板</li>
 *     <li>{@link DistributedLock} — 分布式锁</li>
 *     <li>{@link RateLimiter} — 限流器</li>
 *     <li>{@link RedisCounter} — 原子计数器</li>
 *     <li>{@link RedisCache} — 缓存增强（需 {@code framework.redis.cache.enabled=true}）</li>
 * </ul>
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "framework.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisAutoConfiguration {

    /**
     * 通用 RedisTemplate（JSON 序列化）
     * <p>
     * key 使用 String 序列化，value 使用 GenericJackson2JsonRedisSerializer。
     * 用户可通过注册自定义 {@link RedisTemplate} Bean 覆盖。
     *
     * @param connectionFactory Redis 连接工厂
     * @return 配置好的 RedisTemplate
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * StringRedisTemplate
     * <p>
     * Spring Boot 默认已注册 StringRedisTemplate，此 Bean 定义仅作为后备。
     * 通常不会生效，因为 Spring Boot 自动配置优先注册。
     *
     * @param connectionFactory Redis 连接工厂
     * @return StringRedisTemplate
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 分布式锁
     * <p>
     * 基于 Redis SETNX + Lua 释放脚本实现，确保获取和释放的原子性。
     *
     * @param stringRedisTemplate 字符串 Redis 操作模板
     * @param properties          Redis 配置属性
     * @return RedisLock 实例
     */
    @Bean("redisDistributedLock")
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(DistributedLock.class)
    public DistributedLock distributedLock(StringRedisTemplate stringRedisTemplate,
                                           RedisProperties properties) {
        return new RedisLock(stringRedisTemplate, properties.getLock());
    }

    /**
     * 限流器
     * <p>
     * 基于 Redis 固定窗口 + Lua 脅脚本实现。
     *
     * @param stringRedisTemplate 字符串 Redis 操作模板
     * @param properties          Redis 配置属性
     * @return RedisRateLimiter 实例
     */
    @Bean("redisRateLimiter")
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter rateLimiter(StringRedisTemplate stringRedisTemplate,
                                   RedisProperties properties) {
        return new RedisRateLimiter(stringRedisTemplate, properties.getRateLimit());
    }

    /**
     * 原子计数器
     * <p>
     * 基于 Redis INCR/DECR + Lua 脅脚本实现。
     *
     * @param stringRedisTemplate 字符串 Redis 操作模板
     * @param properties          Redis 配置属性
     * @return RedisCounterImpl 实例
     */
    @Bean("redisCounter")
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(RedisCounter.class)
    public RedisCounter redisCounter(StringRedisTemplate stringRedisTemplate,
                                     RedisProperties properties) {
        return new RedisCounterImpl(stringRedisTemplate, properties.getCounter());
    }

    /**
     * 缓存增强
     * <p>
     * 当 {@code framework.redis.cache.enabled=true}（默认）时激活，
     * 提供 TTL 管理和 null 值缓存防穿透功能。
     *
     * @param redisTemplate 通用 Redis 操作模板
     * @param properties    Redis 配置属性
     * @return RedisCacheImpl 实例
     */
    @Bean("redisCache")
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean(RedisCache.class)
    @ConditionalOnProperty(prefix = "framework.redis.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisCache redisCache(RedisTemplate<String, Object> redisTemplate,
                                 RedisProperties properties) {
        return new RedisCacheImpl(redisTemplate, properties.getCache());
    }
}
