package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * config center demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/config")
public class ConfigDemoController {

    @GetMapping("/source")
    public ApiResponse<Map<String, Object>> source() {
        return ApiResponse.success(Map.of("source", "framework-config", "enabled", true,
                "activeConfigSource", "embedded"));
    }

    @GetMapping({"/governance", "/masked"})
    public ApiResponse<Map<String, Object>> governance() {
        return ApiResponse.success(Map.of("validator", true, "masking", true,
                "comparison", Map.of("raw", "secret", "masked", "***")));
    }

    @GetMapping("/validation")
    public ApiResponse<Map<String, Object>> validation() {
        return ApiResponse.success(Map.of("sensitivityCheck", true));
    }
}
