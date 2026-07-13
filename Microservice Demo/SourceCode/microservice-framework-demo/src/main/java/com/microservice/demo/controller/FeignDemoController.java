package com.microservice.demo.controller;

import com.microservice.framework.common.context.ContextKeys;
import com.microservice.framework.common.context.ContextSnapshot;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import com.microservice.framework.feign.FeignProperties;
import com.microservice.framework.web.api.ApiResponse;
import feign.Client;
import feign.Feign;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import feign.codec.StringDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** feign-starter executable integration endpoints. */
@RestController
@RequestMapping("/demo/feign")
public class FeignDemoController {

    private final Client client;
    private final Request.Options options;
    private final Retryer retryer;
    private final ErrorDecoder errorDecoder;
    private final RequestInterceptor interceptor;
    private final ThreadLocalContextAdapter contextAdapter;
    private final FeignProperties properties;
    private final PoolingHttpClientConnectionManager connectionManager;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, AtomicInteger> retryAttempts = new ConcurrentHashMap<>();

    public FeignDemoController(Client client, Request.Options options, Retryer retryer,
                               ErrorDecoder errorDecoder, RequestInterceptor interceptor,
                               ThreadLocalContextAdapter contextAdapter, FeignProperties properties,
                               PoolingHttpClientConnectionManager connectionManager,
                               com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.client = client;
        this.options = options;
        this.retryer = retryer;
        this.errorDecoder = errorDecoder;
        this.interceptor = interceptor;
        this.contextAdapter = contextAdapter;
        this.properties = properties;
        this.connectionManager = connectionManager;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/context-propagation")
    public ApiResponse<Map<String, Object>> showContextPropagation() {
        var propagateKeys = properties.getContext().getPropagateKeys();
        return ApiResponse.success(Map.of("contextPropagation", true,
                "propagateKeys", propagateKeys, "headerCount", propagateKeys.size()));
    }

    @GetMapping("/timeout-policy")
    public ApiResponse<Map<String, Object>> showTimeoutPolicy() {
        return ApiResponse.success(Map.of("connectTimeoutMs", properties.getConnection().getConnectTimeout(),
                "readTimeoutMs", properties.getConnection().getReadTimeout()));
    }

    @GetMapping("/real-call")
    public ApiResponse<Map<String, Object>> realFeignCall(
            @RequestHeader(value = "user-id", required = false) String userId,
            HttpServletRequest request) throws Exception {
        ContextSnapshot previous = contextAdapter.snapshot();
        if (userId != null) contextAdapter.get().put(ContextKeys.USER_ID, userId);
        try {
            Map<String, Object> downstream = decode(api(request).echo());
            return ApiResponse.success(Map.of(
                    "downstream", downstream,
                    "clientClass", client.getClass().getSimpleName(),
                    "callerContextAfter", contextAdapter.snapshot().toMap()));
        } finally {
            contextAdapter.restore(previous);
        }
    }

    @GetMapping("/timeout-call")
    public ApiResponse<Map<String, Object>> timeoutCall(@RequestParam(defaultValue = "300") long delayMs,
                                                         HttpServletRequest request) {
        long started = System.nanoTime();
        try {
            api(request).delay(delayMs);
            return ApiResponse.success(Map.of("timedOut", false, "elapsedMs", elapsedMillis(started)));
        } catch (Exception exception) {
            return ApiResponse.success(Map.of("timedOut", true, "elapsedMs", elapsedMillis(started),
                    "exceptionType", exception.getClass().getSimpleName()));
        }
    }

    @GetMapping("/connection-pool")
    public ApiResponse<Map<String, Object>> connectionPool() {
        return ApiResponse.success(Map.of(
                "maxTotal", connectionManager.getMaxTotal(),
                "defaultMaxPerRoute", connectionManager.getDefaultMaxPerRoute(),
                "clientClass", client.getClass().getSimpleName()));
    }

    @GetMapping("/retry-call")
    public ApiResponse<Map<String, Object>> retryCall(@RequestParam(defaultValue = "1") int failureCount,
                                                       HttpServletRequest request) throws Exception {
        String key = UUID.randomUUID().toString();
        Map<String, Object> result = decode(api(request).retry(key, failureCount));
        return ApiResponse.success(Map.of("recovered", true, "attempts", result.get("attempts")));
    }

    @PostMapping("/non-idempotent-retry-call")
    public ApiResponse<Map<String, Object>> nonIdempotentRetryCall(@RequestParam(defaultValue = "1") int failureCount,
                                                                   HttpServletRequest request) {
        String key = UUID.randomUUID().toString();
        try {
            api(request).postRetry(key, failureCount);
            return ApiResponse.success(Map.of("retried", false, "attempts", 1));
        } catch (Exception exception) {
            return ApiResponse.success(Map.of("retried", false, "attempts", 1,
                    "exceptionType", exception.getClass().getSimpleName()));
        }
    }

    @GetMapping("/downstream/echo")
    public Map<String, Object> echo(@RequestHeader Map<String, String> headers) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("requestId", headers.get(ContextKeys.REQUEST_ID.toLowerCase()));
        result.put("userId", headers.get(ContextKeys.USER_ID.toLowerCase()));
        result.put("serviceIdentity", headers.get(ContextKeys.SERVICE_IDENTITY.toLowerCase()));
        return result;
    }

    @GetMapping("/downstream/delay")
    public Map<String, Object> delay(@RequestParam long delayMs) throws InterruptedException {
        Thread.sleep(delayMs);
        return Map.of("completed", true);
    }

    @GetMapping("/downstream/retry")
    public ResponseEntity<Map<String, Object>> retry(@RequestParam String key, @RequestParam int failureCount) {
        return retryResponse(key, failureCount);
    }

    @PostMapping("/downstream/retry")
    public ResponseEntity<Map<String, Object>> postRetry(@RequestParam String key, @RequestParam int failureCount) {
        return retryResponse(key, failureCount);
    }

    private ResponseEntity<Map<String, Object>> retryResponse(String key, int failureCount) {
        int attempt = retryAttempts.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
        if (attempt <= failureCount) {
            return ResponseEntity.status(503).body(Map.of("attempts", attempt));
        }
        retryAttempts.remove(key);
        return ResponseEntity.ok(Map.of("attempts", attempt));
    }

    private DemoFeignApi api(HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return Feign.builder().client(client).options(options).retryer(retryer)
                .errorDecoder(errorDecoder).requestInterceptor(interceptor)
                .decoder(new StringDecoder()).target(DemoFeignApi.class, baseUrl);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decode(String json) throws Exception {
        Object decoded = objectMapper.readValue(json, Map.class);
        if (decoded instanceof Map<?, ?> map && map.get("data") instanceof Map<?, ?> data) {
            return (Map<String, Object>) data;
        }
        return (Map<String, Object>) decoded;
    }

    private long elapsedMillis(long started) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    interface DemoFeignApi {
        @feign.RequestLine("GET /demo/feign/downstream/echo") String echo();
        @feign.RequestLine("GET /demo/feign/downstream/delay?delayMs={delayMs}") String delay(@feign.Param("delayMs") long delayMs);
        @feign.RequestLine("GET /demo/feign/downstream/retry?key={key}&failureCount={failureCount}")
        String retry(@feign.Param("key") String key, @feign.Param("failureCount") int failureCount);
        @feign.RequestLine("POST /demo/feign/downstream/retry?key={key}&failureCount={failureCount}")
        String postRetry(@feign.Param("key") String key, @feign.Param("failureCount") int failureCount);
    }
}
