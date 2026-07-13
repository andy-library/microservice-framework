package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import com.microservice.framework.xxljob.api.IdempotentJobHandler;
import com.microservice.framework.xxljob.api.JobExecutionContext;
import com.microservice.framework.xxljob.api.JobResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** xxl-job-starter public capability endpoints. @author Andy Yang */
@RestController
@RequestMapping("/demo/job")
public class JobDemoController {
    private final IdempotentJobHandler handler;

    public JobDemoController(IdempotentJobHandler handler) { this.handler = handler; }

    @PostMapping({"/execute", "/run/daily-report"})
    public ApiResponse<Map<String, Object>> execute(@RequestParam(defaultValue = "1001") int jobId,
                                                     @RequestParam(defaultValue = "demo") String param) {
        JobExecutionContext context = JobExecutionContext.of(jobId, "demo-executor", param);
        boolean duplicateBefore = handler.isDuplicate(context);
        JobResult result = handler.execute(context);
        return ApiResponse.success(Map.of("jobId", jobId, "isDuplicate", duplicateBefore,
                "status", result.getStatus().name(), "message", result.getMessage() == null ? "" : result.getMessage(),
                "executed", result.isSuccess()));
    }

    @PostMapping("/run/idempotent")
    public ApiResponse<Map<String, Object>> idempotent(@RequestParam(defaultValue = "1002") int jobId) {
        JobExecutionContext context = JobExecutionContext.of(jobId, "demo-executor", "idempotency-check");
        handler.execute(context);
        return ApiResponse.success(Map.of("idempotentWorks", handler.isDuplicate(context)));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.success(Map.of("handlerAvailable", handler.getDelegate() != null));
    }
}
