package com.microservice.demo.controller;

import com.microservice.framework.redis.api.DistributedLock;
import com.microservice.framework.redis.api.RateLimiter;
import com.microservice.framework.redis.api.RedisCache;
import com.microservice.framework.redis.api.RedisCounter;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** redis-starter public capability endpoints. @author Andy Yang */
@RestController
@RequestMapping("/demo/redis")
public class RedisDemoController {
    private final RedisCache cache;
    private final DistributedLock lock;
    private final RateLimiter rateLimiter;
    private final RedisCounter counter;

    public RedisDemoController(RedisCache cache, DistributedLock lock,
                               RateLimiter rateLimiter, RedisCounter counter) {
        this.cache = cache;
        this.lock = lock;
        this.rateLimiter = rateLimiter;
        this.counter = counter;
    }

    @PutMapping("/cache/{key}")
    public ApiResponse<Map<String, Object>> put(@PathVariable String key, @RequestBody Map<String, Object> body) {
        cache.put(key, body);
        return ApiResponse.success(Map.of("operation", "put", "key", key));
    }

    @GetMapping("/cache/{key}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String key) {
        Object value = cache.get(key);
        return ApiResponse.success(Map.of("found", value != null, "value", value == null ? Map.of() : value));
    }

    @PostMapping("/cache/batch-delete")
    public ApiResponse<Map<String, Object>> batchDelete(@RequestBody Iterable<String> keys) {
        List<String> boundedKeys = new ArrayList<>();
        keys.forEach(boundedKeys::add);
        cache.evictAll(boundedKeys);
        return ApiResponse.success(Map.of("operation", "batchDelete", "count", boundedKeys.size()));
    }

    @PostMapping("/cache/pattern-delete")
    public ApiResponse<Map<String, Object>> patternDelete(@RequestBody Map<String, Object> body) {
        String pattern = String.valueOf(body.get("pattern"));
        return ApiResponse.success(Map.of("operation", "patternDelete", "deleted", cache.evictByPattern(pattern)));
    }

    @PostMapping({"/lock", "/lock/{key}"})
    public ApiResponse<Map<String, Object>> lock(@PathVariable(required = false) String key) {
        String lockKey = key == null ? "demo-lock" : key;
        boolean acquired = lock.tryLock(lockKey);
        boolean released = acquired && lock.unlock(lockKey);
        return ApiResponse.success(Map.of("key", lockKey, "locked", acquired, "unlocked", released));
    }

    @GetMapping({"/rate-limit", "/rate-limit/{key}"})
    public ApiResponse<Map<String, Object>> rateLimit(@PathVariable(required = false) String key) {
        String rateKey = key == null ? "demo-rate-limit" : key;
        boolean acquired = rateLimiter.tryAcquire(rateKey);
        return ApiResponse.success(Map.of("key", rateKey, "allowed", acquired, "acquired", acquired,
                "availablePermits", rateLimiter.getAvailablePermits(rateKey)));
    }

    @PostMapping("/counter/increment")
    public ApiResponse<Map<String, Object>> counterIncrement(@RequestParam(defaultValue = "demo-counter") String key) {
        return ApiResponse.success(Map.of("key", key, "newValue", counter.increment(key)));
    }

    @DeleteMapping("/cache/{key}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable String key) {
        return ApiResponse.success(Map.of("operation", "delete", "key", key, "deleted", cache.evict(key)));
    }
}
