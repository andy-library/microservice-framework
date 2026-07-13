package com.microservice.demo.controller;

import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import com.microservice.framework.common.id.IdGenerator;
import com.microservice.framework.common.page.PageResult;
import com.microservice.framework.common.time.FrameworkClock;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * common-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/common")
public class CommonDemoController {

    private final IdGenerator idGenerator;
    private final FrameworkClock frameworkClock;
    private final ThreadLocalContextAdapter contextAdapter;

    public CommonDemoController(IdGenerator idGenerator, FrameworkClock frameworkClock,
                                ThreadLocalContextAdapter contextAdapter) {
        this.idGenerator = idGenerator;
        this.frameworkClock = frameworkClock;
        this.contextAdapter = contextAdapter;
    }

    @GetMapping("/id")
    public ApiResponse<Map<String, Object>> id() {
        return ApiResponse.success(Map.of("id", idGenerator.generate(), "type", "snowflake", "source", "IdGenerator"));
    }

    @GetMapping("/time")
    public ApiResponse<Map<String, Object>> time() {
        return ApiResponse.success(Map.of("time", frameworkClock.now().toString(),
                "zoneId", frameworkClock.getZoneId().getId(), "source", "FrameworkClock"));
    }

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page() {
        PageResult<String> page = PageResult.of(3, List.of("one", "two", "three"), 1, 10);
        return ApiResponse.success(Map.of("items", page.getItems(), "page", page.getPageNumber(),
                "size", page.getPageSize(), "pageNumber", page.getPageNumber(),
                "pageSize", page.getPageSize(), "total", page.getTotal()));
    }

    @GetMapping("/context")
    public ApiResponse<Map<String, Object>> context() {
        return ApiResponse.success(Map.of("hasContext", contextAdapter.get() != null,
                "contextSize", contextAdapter.get().toMap().size(), "source", "ThreadLocalContextAdapter"));
    }
}
