package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * object-storage-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/storage")
public class StorageDemoController {

    private final Map<String, String> objects = new ConcurrentHashMap<>();

    @PostMapping("/object")
    public ApiResponse<Map<String, Object>> put(@RequestBody(required = false) Map<String, Object> body) {
        String key = body == null ? UUID.randomUUID().toString() : String.valueOf(body.getOrDefault("key", UUID.randomUUID().toString()));
        String content = body == null ? "" : String.valueOf(body.getOrDefault("content", ""));
        objects.put(key, content);
        return ApiResponse.success(Map.of("key", key, "objectKey", key, "stored", true, "operation", "upload"));
    }

    @GetMapping("/object/{key}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String key) {
        return ApiResponse.success(Map.of("objectKey", key, "found", objects.containsKey(key), "operation", "download"));
    }

    @GetMapping("/object/{key}/presigned-url")
    public ApiResponse<Map<String, Object>> presigned(@PathVariable String key) {
        return ApiResponse.success(Map.of("objectKey", key, "implementation", "embedded"));
    }

    @DeleteMapping("/object/{key}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable String key) {
        objects.remove(key);
        return ApiResponse.success(Map.of("objectKey", key, "operation", "delete"));
    }
}
