package com.microservice.demo.controller;

import com.microservice.framework.redis.api.DistributedLock;
import com.microservice.framework.redis.api.RateLimiter;
import com.microservice.framework.redis.api.RedisCache;
import com.microservice.framework.redis.api.RedisCounter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * redis-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/redis")
public class RedisTestController extends RedisDemoController {
    public RedisTestController(RedisCache cache, DistributedLock lock, RateLimiter rateLimiter, RedisCounter counter) {
        super(cache, lock, rateLimiter, counter);
    }
}
