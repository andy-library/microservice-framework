package com.microservice.framework.redis.core;

final class RedisKeys {

    private RedisKeys() {
    }

    static String prefixed(String prefix, String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Redis key must not be null or blank");
        }
        return prefix + key;
    }
}
