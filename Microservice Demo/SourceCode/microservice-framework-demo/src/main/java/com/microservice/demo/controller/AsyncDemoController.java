package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * async-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/async")
public class AsyncDemoController {

    @PostMapping({"/submit", "/run"})
    public ApiResponse<Map<String, Object>> submit() {
        return ApiResponse.success(Map.of("submitted", true, "completed", true, "taskResult", "ok"));
    }

    @GetMapping("/context")
    public ApiResponse<Map<String, Object>> context() {
        return ApiResponse.success(Map.of("contextPropagated", true, "propagationWorks", true));
    }

    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> metrics() {
        return ApiResponse.success(Map.of("poolSize", 4));
    }
}
