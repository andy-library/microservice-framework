package com.microservice.demo.controller;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Request ID 演示 Controller
 * 专门用于验证 Request ID 三级降级逻辑：
 * 1. TraceId 优先 (如果已存在 Trace 上下文)
 * 2. 网关 Header 次之 (如 X-Request-ID)
 * 3. UUID 兜底 (自动生成)
 * 
 * @author Andy Yang
 */
@RestController
@RequestMapping("/api/demo/request-id")
public class RequestIdDemoController {

    private static final Logger log = LoggerFactory.getLogger(RequestIdDemoController.class);

    @Autowired
    private Tracer tracer;

    /**
     * 获取当前请求的所有 ID 信息
     * 用于验证 MDC 中的 requestId 来源
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRequestIdInfo(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 从 MDC 获取 Request ID (由 GatewayIntegrationFilter 注入)
        String requestIdFromMdc = MDC.get("requestId");
        if (requestIdFromMdc == null || requestIdFromMdc.isBlank()) {
            requestIdFromMdc = request.getHeader("X-Request-ID");
        }
        if (requestIdFromMdc == null || requestIdFromMdc.isBlank()) {
            requestIdFromMdc = UUID.randomUUID().toString();
        }
        result.put("requestId", requestIdFromMdc);

        // 2. 从 Trace 上下文获取信息
        if (tracer.currentSpan() != null) {
            String traceId = tracer.currentSpan().context().traceId();
            String spanId = tracer.currentSpan().context().spanId();
            result.put("traceId", traceId);
            result.put("spanId", spanId);

            // 判断 Request ID 来源
            if (traceId != null && traceId.equals(requestIdFromMdc)) {
                result.put("source", "TraceId (优先级1)");
            }
        }

        // 3. 检查是否来自 Header
        String headerValue = request.getHeader("X-Request-ID");
        result.put("x-request-id-header", headerValue != null ? headerValue : "not-provided");
        if (headerValue != null && headerValue.equals(requestIdFromMdc)) {
            result.put("source", "Gateway Header (优先级2)");
        }

        // 4. 如果 source 未设置，说明是 UUID 兜底
        if (!result.containsKey("source")) {
            result.put("source", "UUID Fallback (优先级3)");
        }

        // 记录日志 (日志中会自动包含 traceId, spanId, requestId)
        log.info("Request ID 验证 requestId={} source={}", requestIdFromMdc, result.get("source"));

        return ResponseEntity.ok(result);
    }

    /**
     * 测试场景：模拟网关透传 Request ID
     * 使用方式：curl -H "X-Request-ID: my-custom-id"
     * http://localhost:8080/api/demo/request-id/gateway-test
     */
    @GetMapping("/gateway-test")
    public ResponseEntity<Map<String, Object>> gatewayTest(
            @RequestHeader(value = "X-Request-ID", required = false) String gatewayRequestId,
            HttpServletRequest request) {

        Map<String, Object> result = new LinkedHashMap<>();
        String mdcRequestId = MDC.get("requestId");
        if (mdcRequestId == null || mdcRequestId.isBlank()) {
            mdcRequestId = gatewayRequestId != null ? gatewayRequestId : UUID.randomUUID().toString();
        }

        result.put("input_header", gatewayRequestId != null ? gatewayRequestId : "not-provided");
        result.put("mdc_request_id", mdcRequestId);

        // 如果有 TraceId，它会覆盖 Header
        if (tracer.currentSpan() != null) {
            String traceId = tracer.currentSpan().context().traceId();
            result.put("trace_id", traceId);
            result.put("explanation", "TraceId 存在时优先使用 TraceId 作为 RequestId");
        } else if (gatewayRequestId != null && gatewayRequestId.equals(mdcRequestId)) {
            result.put("explanation", "无 TraceId，使用网关透传的 X-Request-ID");
        } else {
            result.put("explanation", "全缺失，自动生成 UUID");
        }

        log.info("网关透传测试 gatewayRequestId={} mdcRequestId={}", gatewayRequestId, mdcRequestId);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取完整的可观测性上下文信息
     */
    @GetMapping("/full-context")
    public ResponseEntity<Map<String, Object>> getFullContext(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // MDC 信息
        Map<String, String> mdcContext = new LinkedHashMap<>();
        mdcContext.put("traceId", MDC.get("traceId"));
        mdcContext.put("spanId", MDC.get("spanId"));
        mdcContext.put("requestId", MDC.get("requestId"));
        result.put("mdc", mdcContext);

        // Tracer 信息
        if (tracer.currentSpan() != null) {
            Map<String, String> tracerContext = new LinkedHashMap<>();
            tracerContext.put("traceId", tracer.currentSpan().context().traceId());
            tracerContext.put("spanId", tracer.currentSpan().context().spanId());
            tracerContext.put("sampled", String.valueOf(tracer.currentSpan().context().sampled()));
            result.put("tracer", tracerContext);
        }

        // Request Attribute (由 Filter 设置)
        Object requestIdAttr = request.getAttribute("requestId");
        result.put("requestAttribute", requestIdAttr != null ? requestIdAttr : "not-set");

        // Headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Request-ID", request.getHeader("X-Request-ID"));
        headers.put("traceparent", request.getHeader("traceparent"));
        result.put("headers", headers);

        log.info("完整上下文查询");

        return ResponseEntity.ok(result);
    }
}
