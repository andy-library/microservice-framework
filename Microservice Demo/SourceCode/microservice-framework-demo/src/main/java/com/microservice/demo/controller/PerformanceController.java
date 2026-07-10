package com.microservice.demo.controller;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.microservice.framework.async.api.AsyncTaskExecutor;
import com.microservice.framework.redis.api.RedisCache;
import com.microservice.framework.web.api.ApiResponse;

import io.micrometer.tracing.Tracer;

/**
 * Performance testing endpoints for measuring framework overhead and infrastructure latency.
 *
 * Each endpoint targets a specific layer so that k6 or similar tools can compare
 * bare Spring Boot throughput against the full framework chain.
 *
 * When running in full-embedded or full-middleware profile, all beans (Tracer,
 * RedisCache, AsyncTaskExecutor, DataSource) are available and endpoints use
 * real infrastructure. In local-smoke profile, beans may be null and endpoints
 * use simulated fallbacks.
 */
@RestController
@RequestMapping("/perf")
public class PerformanceController {

    @Autowired(required = false)
    private RedisCache redisCache;

    @Autowired(required = false)
    private AsyncTaskExecutor asyncTaskExecutor;

    @Autowired(required = false)
    private Tracer tracer;

    // ---------------------------------------------------------------
    // 1. Plain endpoint — NO framework overhead
    // ---------------------------------------------------------------

    /**
     * Raw Spring Boot baseline. Returns a simple Map with no ApiResponse wrapping,
     * no requestId, no logging interceptor — just @ResponseBody Map serialization.
     */
    @GetMapping("/plain")
    @ResponseBody
    public Map<String, Object> plain() {
        Map<String, Object> result = new HashMap<>(2);
        result.put("status", "ok");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    // ---------------------------------------------------------------
    // 2. Framework-web endpoint — full ApiResponse chain
    // ---------------------------------------------------------------

    /**
     * Goes through the entire framework web chain: requestId injection,
     * observability interceptors, logging, and ApiResponse JSON serialization.
     */
    @GetMapping("/framework-web")
    public ApiResponse<Map<String, Object>> frameworkWeb() {
        Map<String, Object> data = new LinkedHashMap<>(2);
        data.put("timestamp", System.currentTimeMillis());
        data.put("endpoint", "framework-web");
        return ApiResponse.success(data);
    }

    // ---------------------------------------------------------------
    // 3. Cache-hit endpoint — conditional Redis or simulated
    // ---------------------------------------------------------------

    /**
     * If RedisCache bean is available, attempts a real cache read.
     * Otherwise returns a simulated cache-hit result so the endpoint
     * still works in non-Redis deployments.
     */
    @GetMapping("/cache-hit")
    public ApiResponse<Map<String, Object>> cacheHit() {
        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("timestamp", System.currentTimeMillis());

        if (redisCache != null) {
            String key = "perf:cache-hit:test";
            Object cached = redisCache.get(key);
            data.put("source", "redis");
            data.put("hit", cached != null);
            if (cached == null) {
                // Seed the key so subsequent calls are cache hits
                redisCache.put(key, "perf-test-value");
                data.put("value", "perf-test-value (seeded)");
            } else {
                data.put("value", cached);
            }
        } else {
            // Simulated cache hit — no Redis available
            data.put("source", "simulated");
            data.put("hit", true);
            data.put("value", "simulated-cache-value");
        }

        return ApiResponse.success(data);
    }

    // ---------------------------------------------------------------
    // 4. Full-chain-async endpoint — complete pipeline simulation
    // ---------------------------------------------------------------

    /**
     * Simulates a full request lifecycle: auth context read, logging/trace metadata,
     * cache lookup, DB read, and async task submission.
     *
     * When Tracer/RedisCache/AsyncTaskExecutor are available (full-embedded/full-middleware),
     * uses real infrastructure. Otherwise falls back to simulated values for local-smoke.
     *
     * POST is used because async task submission is a state-changing operation.
     */
    @PostMapping("/full-chain-async")
    public ApiResponse<Map<String, Object>> fullChainAsync() {
        Map<String, Object> data = new LinkedHashMap<>(8);
        long start = System.currentTimeMillis();
        data.put("timestamp", start);

        // --- Auth context ---
        data.put("authContext", "perf-user");

        // --- Trace metadata — use real Tracer when available ---
        String traceId;
        if (tracer != null && tracer.currentSpan() != null) {
            traceId = tracer.currentSpan().context().traceId();
        } else {
            traceId = UUID.randomUUID().toString();
        }
        data.put("traceId", traceId);

        // --- Cache lookup — use real RedisCache when available ---
        if (redisCache != null) {
            Object cached = redisCache.get("perf:full-chain:config");
            data.put("cacheLookup", cached != null ? "hit" : "miss");
            if (cached == null) {
                // Seed the key so subsequent calls are cache hits
                redisCache.put("perf:full-chain:config", "perf-config-value");
            }
        } else {
            data.put("cacheLookup", "simulated-hit");
        }

        // --- DB read — currently in-memory, no JPA repository call in perf path ---
        data.put("dbRead", "in-memory-entity");

        // --- Async task submission — use real executor when available ---
        String asyncTaskId = UUID.randomUUID().toString();
        if (asyncTaskExecutor != null) {
            CompletableFuture.runAsync(() -> {
                // Minimal async work — realistic for a perf baseline
            }, asyncTaskExecutor);
            data.put("asyncTaskSubmitted", true);
        } else {
            data.put("asyncTaskSubmitted", false);
        }
        data.put("asyncTaskId", asyncTaskId);
        data.put("messageAccepted", true);

        long elapsed = System.currentTimeMillis() - start;
        data.put("elapsedMs", elapsed);

        return ApiResponse.success(data);
    }

    // ---------------------------------------------------------------
    // 5. Health endpoint — JVM and thread pool metrics
    // ---------------------------------------------------------------

    /**
     * Returns JVM runtime stats, memory metrics, and thread pool info
     * if the AsyncTaskExecutor bean is available.
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>(16);

        // --- JVM runtime stats ---
        Runtime runtime = Runtime.getRuntime();
        data.put("jvmAvailableProcessors", runtime.availableProcessors());
        data.put("jvmTotalMemory", runtime.totalMemory());
        data.put("jvmFreeMemory", runtime.freeMemory());
        data.put("jvmMaxMemory", runtime.maxMemory());
        data.put("jvmUsedMemory", runtime.totalMemory() - runtime.freeMemory());

        // --- Memory MXBean ---
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        data.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
        data.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
        data.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());

        // --- Thread pool info (if available) ---
        if (asyncTaskExecutor != null) {
            Map<String, Object> poolInfo = new LinkedHashMap<>(4);
            poolInfo.put("activeCount", asyncTaskExecutor.getActiveCount());
            poolInfo.put("poolSize", asyncTaskExecutor.getPoolSize());
            poolInfo.put("queueSize", asyncTaskExecutor.getQueueSize());
            poolInfo.put("completedTaskCount", asyncTaskExecutor.getCompletedTaskCount());
            data.put("asyncThreadPool", poolInfo);
        } else {
            data.put("asyncThreadPool", "not-available");
        }

        // --- System metrics ---
        data.put("timestamp", System.currentTimeMillis());
        data.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());

        return ApiResponse.success(data);
    }
}
