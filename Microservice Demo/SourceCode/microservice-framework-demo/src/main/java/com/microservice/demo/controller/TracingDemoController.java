package com.microservice.demo.controller;

import com.microservice.framework.logging.util.Log;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 追踪演示 Controller
 * 专门用于演示分布式追踪功能
 */
@RestController
@RequestMapping("/api/demo/tracing")
public class TracingDemoController {

    @Autowired
    private Tracer tracer;

    /**
     * 获取当前 Trace 信息
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, String>> getCurrentTrace() {
        Map<String, String> traceInfo = new HashMap<>();

        if (tracer.currentSpan() != null) {
            traceInfo.put("traceId", tracer.currentSpan().context().traceId());
            traceInfo.put("spanId", tracer.currentSpan().context().spanId());
            traceInfo.put("sampled", String.valueOf(tracer.currentSpan().context().sampled()));
        } else {
            traceInfo.put("message", "No active span");
        }

        // Add Request ID from MDC
        String requestId = org.slf4j.MDC.get("requestId");
        if (requestId != null) {
            traceInfo.put("requestId", requestId);
        }

        Log.info("获取当前 Trace 信息").with("traceInfo", traceInfo).log();

        return ResponseEntity.ok(traceInfo);
    }

    /**
     * 演示 Baggage 传播
     */
    @GetMapping("/baggage")
    public ResponseEntity<Map<String, String>> baggageDemo(
            @RequestHeader(value = "user-id", required = false) String userId,
            @RequestHeader(value = "tenant-id", required = false) String tenantId) {

        Map<String, String> result = new HashMap<>();
        result.put("userId", userId != null ? userId : "not-provided");
        result.put("tenantId", tenantId != null ? tenantId : "not-provided");

        if (tracer.currentSpan() != null) {
            result.put("traceId", tracer.currentSpan().context().traceId());
        }

        Log.info("Baggage 传播演示")
                .with("userId", userId)
                .with("tenantId", tenantId)
                .log();

        return ResponseEntity.ok(result);
    }

    /**
     * 演示 Trace 采样
     */
    @GetMapping("/sampling")
    public ResponseEntity<Map<String, Object>> samplingDemo() {
        Map<String, Object> result = new HashMap<>();

        if (tracer.currentSpan() != null) {
            boolean sampled = tracer.currentSpan().context().sampled();
            result.put("sampled", sampled);
            result.put("traceId", tracer.currentSpan().context().traceId());
            result.put("message", sampled ? "This trace is sampled - all logs will be recorded"
                    : "This trace is NOT sampled - only INFO+ logs will be recorded");

            // 记录不同级别的日志
            Log.info("Demonstrating baggage propagation").log();
            Log.info("INFO log - always recorded").log();
            Log.warn("WARN log - always recorded").log();
        } else {
            result.put("message", "No active span");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 演示 @SpanTag 注解
     */
    @GetMapping("/tag")
    public ResponseEntity<String> spanTagDemo(
            @com.microservice.framework.observability.annotation.SpanTag("demo.tag") @RequestParam("value") String value) {
        Log.info("SpanTag 演示").with("value", value).log();
        return ResponseEntity.ok("Tag added: " + value);
    }
}
