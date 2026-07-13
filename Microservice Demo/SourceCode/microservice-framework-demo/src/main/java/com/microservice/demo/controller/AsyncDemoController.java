package com.microservice.demo.controller;

import com.microservice.framework.async.api.AsyncTaskExecutor;
import com.microservice.framework.common.context.ContextKeys;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** async-starter public capability endpoints. @author Andy Yang */
@RestController
@RequestMapping("/demo/async")
public class AsyncDemoController {
    private final AsyncTaskExecutor executor;
    private final ThreadLocalContextAdapter contextAdapter;

    public AsyncDemoController(AsyncTaskExecutor executor, ThreadLocalContextAdapter contextAdapter) {
        this.executor = executor;
        this.contextAdapter = contextAdapter;
    }

    @PostMapping({"/submit", "/run"})
    public ApiResponse<Map<String, Object>> submit() throws Exception {
        CompletableFuture<String> result = new CompletableFuture<>();
        executor.execute(() -> result.complete("ok"));
        return ApiResponse.success(Map.of("submitted", true, "completed", true,
                "taskResult", result.get(5, TimeUnit.SECONDS)));
    }

    @GetMapping("/context")
    public ApiResponse<Map<String, Object>> context() throws Exception {
        String expected = contextAdapter.get().get(ContextKeys.REQUEST_ID);
        if (expected == null) {
            expected = "async-demo-context";
            contextAdapter.get().put(ContextKeys.REQUEST_ID, expected);
        }
        String expectedValue = expected;
        CompletableFuture<String> observed = new CompletableFuture<>();
        executor.execute(() -> observed.complete(contextAdapter.get().get(ContextKeys.REQUEST_ID)));
        String actual = observed.get(5, TimeUnit.SECONDS);
        boolean propagated = java.util.Objects.equals(expectedValue, actual);
        return ApiResponse.success(Map.of("expected", expectedValue,
                "actual", actual == null ? "" : actual, "contextPropagated", propagated,
                "propagationWorks", propagated));
    }

    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> metrics() {
        return ApiResponse.success(Map.of("poolSize", executor.getPoolSize(), "activeCount", executor.getActiveCount(),
                "queueSize", executor.getQueueSize(), "completedTaskCount", executor.getCompletedTaskCount()));
    }
}
