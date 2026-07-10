package com.microservice.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * observability-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/observability")
public class ObservabilityTestController {

    private final TracingDemoController tracingDemoController;
    private final MetricsDemoController metricsDemoController;
    private final HealthDemoController healthDemoController;
    private final RequestIdDemoController requestIdDemoController;

    public ObservabilityTestController(TracingDemoController tracingDemoController,
                                       MetricsDemoController metricsDemoController,
                                       HealthDemoController healthDemoController,
                                       RequestIdDemoController requestIdDemoController) {
        this.tracingDemoController = tracingDemoController;
        this.metricsDemoController = metricsDemoController;
        this.healthDemoController = healthDemoController;
        this.requestIdDemoController = requestIdDemoController;
    }

    @GetMapping("/tracing/current")
    public ResponseEntity<Map<String, String>> currentTrace() {
        return tracingDemoController.getCurrentTrace();
    }

    @GetMapping("/tracing/baggage")
    public ResponseEntity<Map<String, String>> baggage(@RequestHeader(value = "user-id", required = false) String userId,
                                                       @RequestHeader(value = "tenant-id", required = false) String tenantId) {
        return tracingDemoController.baggageDemo(userId, tenantId);
    }

    @GetMapping("/tracing/sampling")
    public ResponseEntity<Map<String, Object>> sampling() {
        return tracingDemoController.samplingDemo();
    }

    @GetMapping("/tracing/tag")
    public ResponseEntity<String> tag(@RequestParam("value") String value) {
        return tracingDemoController.spanTagDemo(value);
    }

    @GetMapping("/metrics/counter")
    public ResponseEntity<Map<String, Object>> counter(@RequestParam(defaultValue = "mobile") String channel) {
        return metricsDemoController.incrementCounter(channel);
    }

    @GetMapping("/metrics/timer")
    public ResponseEntity<Map<String, Object>> timer() {
        return metricsDemoController.recordTimer();
    }

    @GetMapping("/health/custom")
    public ResponseEntity<Map<String, Object>> customHealth() {
        return healthDemoController.customHealth();
    }

    @GetMapping("/health/endpoints")
    public ResponseEntity<Map<String, String>> actuatorEndpoints() {
        return healthDemoController.getEndpoints();
    }

    @GetMapping("/request-id")
    public ResponseEntity<Map<String, Object>> requestId(HttpServletRequest request) {
        return requestIdDemoController.getRequestIdInfo(request);
    }

    @GetMapping("/request-id/gateway-test")
    public ResponseEntity<Map<String, Object>> gatewayRequestId(
            @RequestHeader(value = "X-Request-ID", required = false) String gatewayRequestId,
            HttpServletRequest request) {
        return requestIdDemoController.gatewayTest(gatewayRequestId, request);
    }

    @GetMapping("/request-id/full-context")
    public ResponseEntity<Map<String, Object>> fullContext(HttpServletRequest request) {
        return requestIdDemoController.getFullContext(request);
    }
}
