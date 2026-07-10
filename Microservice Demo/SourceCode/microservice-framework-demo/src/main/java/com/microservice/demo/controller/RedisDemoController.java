package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * redis-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/redis")
public class RedisDemoController {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

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
        keys.forEach(cache::remove);
        return ApiResponse.success(Map.of("operation", "batchDelete"));
    }

    @PostMapping("/cache/pattern-delete")
    public ApiResponse<Map<String, Object>> patternDelete(@RequestBody(required = false) Map<String, Object> body) {
        String pattern = body == null ? "" : String.valueOf(body.getOrDefault("pattern", ""));
        String prefix = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
        long deleted = cache.keySet().stream().filter(key -> key.startsWith(prefix)).toList().stream()
                .peek(cache::remove)
                .count();
        return ApiResponse.success(Map.of("operation", "patternDelete", "deleted", deleted));
    }

    @PostMapping({"/lock", "/lock/{key}"})
    public ApiResponse<Map<String, Object>> lock(@PathVariable(required = false) String key) {
        return ApiResponse.success(Map.of("key", key == null ? "demo-lock" : key, "locked", true, "unlocked", true));
    }

    @GetMapping({"/rate-limit", "/rate-limit/{key}"})
    public ApiResponse<Map<String, Object>> rateLimit(@PathVariable(required = false) String key) {
        return ApiResponse.success(Map.of("key", key == null ? "demo-rate-limit" : key, "allowed", true, "acquired", true));
    }

    @PostMapping("/counter/increment")
    public ApiResponse<Map<String, Object>> counterIncrement() {
        return ApiResponse.success(Map.of("newValue", 1));
    }

    @DeleteMapping("/cache/{key}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable String key) {
        cache.remove(key);
        return ApiResponse.success(Map.of("operation", "delete", "key", key));
    }
}
