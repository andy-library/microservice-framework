package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * web-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/web")
public class WebDemoController {

    @GetMapping("/success")
    public ApiResponse<Map<String, Object>> success() {
        return ApiResponse.success(Map.of("message", "ok", "framework", "working"));
    }

    @GetMapping("/business-error")
    public ResponseEntity<ApiResponse<Object>> businessError() {
        return ResponseEntity.badRequest().body(ApiResponse.error(40001, "demo business error"));
    }

    @PostMapping("/validation-error")
    public ApiResponse<Map<String, Object>> validation(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(body);
    }

    @GetMapping("/request-id")
    public ApiResponse<Map<String, Object>> requestId() {
        return ApiResponse.success(Map.of("requestId", UUID.randomUUID().toString()));
    }
}
