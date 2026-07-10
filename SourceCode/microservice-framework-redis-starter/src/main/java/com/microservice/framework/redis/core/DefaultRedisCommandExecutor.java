package com.microservice.framework.redis.core;

import com.microservice.framework.redis.api.RedisCommandExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

/**
 * Default Redis command executor backed by {@link StringRedisTemplate}.
 *
 * @author Andy Yang
 */
public class DefaultRedisCommandExecutor implements RedisCommandExecutor {

    private final StringRedisTemplate stringRedisTemplate;

    public DefaultRedisCommandExecutor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Boolean setIfAbsent(String key, String value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return stringRedisTemplate.opsForValue().setIfAbsent(key, value);
        }
        return stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public Long getExpire(String key) {
        return stringRedisTemplate.getExpire(key);
    }

    @Override
    public Object executeScript(String scriptContent, Class<?> returnType, List<String> keys, Object... args) {
        DefaultRedisScript<?> script = new DefaultRedisScript<>(scriptContent, returnType);
        return stringRedisTemplate.execute(script, keys, args);
    }
}
