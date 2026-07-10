package com.microservice.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查演示 Controller
 * 演示 Actuator 端点使用
 */
@RestController
@RequestMapping("/api/demo/health")
public class HealthDemoController {

    /**
     * 自定义健康检查端点
     */
    @GetMapping("/custom")
    public ResponseEntity<Map<String, Object>> customHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "microservice-framework-demo");
        health.put("timestamp", System.currentTimeMillis());

        Map<String, String> details = new HashMap<>();
        details.put("database", "N/A - in-memory");
        details.put("cache", "N/A");
        health.put("details", details);

        return ResponseEntity.ok(health);
    }

    /**
     * 提示如何使用 Actuator 端点
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, String>> getEndpoints() {
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/actuator/health");
        endpoints.put("info", "/actuator/info");
        endpoints.put("metrics", "/actuator/metrics");
        endpoints.put("prometheus", "/actuator/prometheus");
        endpoints.put("loggers", "/actuator/loggers");
        endpoints.put("loggers-detail", "/actuator/loggers/com.microservice.demo");

        return ResponseEntity.ok(endpoints);
    }
}
