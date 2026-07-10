package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * security-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/security")
public class SecurityDemoController {

    @GetMapping("/public")
    public ApiResponse<Map<String, Object>> publicEndpoint() {
        return ApiResponse.success(Map.of("access", "public"));
    }

    @GetMapping("/user")
    public ApiResponse<Map<String, Object>> userEndpoint() {
        return ApiResponse.success(Map.of("access", "user"));
    }

    @GetMapping("/admin")
    public ApiResponse<Map<String, Object>> adminEndpoint() {
        return ApiResponse.success(Map.of("access", "admin"));
    }

    @GetMapping("/internal")
    public ApiResponse<Map<String, Object>> internalEndpoint() {
        return ApiResponse.success(Map.of("access", "internal"));
    }
}
