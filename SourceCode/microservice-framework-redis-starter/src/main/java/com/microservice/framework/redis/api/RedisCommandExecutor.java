package com.microservice.framework.redis.api;

import java.time.Duration;
import java.util.List;

/**
 * Redis command abstraction for low-level atomic operations used by framework
 * components and demo embedded providers.
 *
 * @author Andy Yang
 */
public interface RedisCommandExecutor {

    Boolean setIfAbsent(String key, String value, Duration ttl);

    String get(String key);

    Boolean hasKey(String key);

    Long getExpire(String key);

    Object executeScript(String scriptContent, Class<?> returnType, List<String> keys, Object... args);
}
