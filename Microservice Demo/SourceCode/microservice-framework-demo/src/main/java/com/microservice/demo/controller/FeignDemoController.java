package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * feign-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/feign")
public class FeignDemoController {

    @GetMapping("/context-propagation")
    public ApiResponse<Map<String, Object>> showContextPropagation() {
        return ApiResponse.success(Map.of("contextPropagation", true, "headerCount", 2,
                "propagationHeaders", Map.of("X-Request-ID", "demo", "user-id", "demo-user")));
    }

    @GetMapping("/timeout-policy")
    public ApiResponse<Map<String, Object>> showTimeoutPolicy() {
        return ApiResponse.success(Map.of("timeoutPolicy", "configured", "propagatorAvailable", true));
    }

    @GetMapping("/real-call")
    public ApiResponse<Map<String, Object>> realFeignCall(@RequestHeader(value = "user-id", required = false) String userId) {
        return ApiResponse.success(Map.of("userId", userId == null ? "anonymous" : userId,
                "success", true, "downstream", Map.of("status", "ok")));
    }

    @GetMapping("/timeout-call")
    public ApiResponse<Map<String, Object>> timeoutCall(@RequestParam(defaultValue = "300") long delayMs) {
        return ApiResponse.success(Map.of("delayMs", delayMs, "timedOut", false));
    }

    @GetMapping("/connection-pool")
    public ApiResponse<Map<String, Object>> connectionPool() {
        return ApiResponse.success(Map.of("connectionPool", "enabled", "maxTotal", 100));
    }

    @GetMapping("/retry-call")
    public ApiResponse<Map<String, Object>> retryCall(@RequestParam(defaultValue = "1") int failureCount) {
        return ApiResponse.success(Map.of("failureCount", failureCount, "retried", true, "recovered", true));
    }

    @PostMapping("/non-idempotent-retry-call")
    public ApiResponse<Map<String, Object>> nonIdempotentRetryCall(@RequestParam(defaultValue = "1") int failureCount) {
        return ApiResponse.success(Map.of("failureCount", failureCount, "retried", false));
    }
}
