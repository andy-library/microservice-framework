package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

/**
 * feign-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/feign")
public class FeignTestController {

    private final FeignDemoController feignDemoController;

    public FeignTestController(FeignDemoController feignDemoController) {
        this.feignDemoController = feignDemoController;
    }

    @GetMapping("/context-propagation")
    public ApiResponse<Map<String, Object>> contextPropagation() {
        return feignDemoController.showContextPropagation();
    }

    @GetMapping("/timeout-policy")
    public ApiResponse<Map<String, Object>> timeoutPolicy() {
        return feignDemoController.showTimeoutPolicy();
    }

    @GetMapping("/real-call")
    public ApiResponse<Map<String, Object>> realCall(
            @RequestHeader(value = "user-id", required = false) String userId,
            HttpServletRequest request) throws Exception {
        return feignDemoController.realFeignCall(userId, request);
    }

    @GetMapping("/timeout-call")
    public ApiResponse<Map<String, Object>> timeoutCall(@RequestParam(defaultValue = "300") long delayMs,
                                                         HttpServletRequest request) {
        return feignDemoController.timeoutCall(delayMs, request);
    }

    @GetMapping("/connection-pool")
    public ApiResponse<Map<String, Object>> connectionPool() {
        return feignDemoController.connectionPool();
    }

    @GetMapping("/retry-call")
    public ApiResponse<Map<String, Object>> retryCall(@RequestParam(defaultValue = "1") int failureCount,
                                                       HttpServletRequest request) throws Exception {
        return feignDemoController.retryCall(failureCount, request);
    }

    @PostMapping("/non-idempotent-retry-call")
    public ApiResponse<Map<String, Object>> nonIdempotentRetryCall(@RequestParam(defaultValue = "1") int failureCount,
                                                                   HttpServletRequest request) {
        return feignDemoController.nonIdempotentRetryCall(failureCount, request);
    }
}
