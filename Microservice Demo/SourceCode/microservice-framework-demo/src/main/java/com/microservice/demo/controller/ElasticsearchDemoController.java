package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * elasticsearch-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping({"/demo/elasticsearch", "/demo/es"})
public class ElasticsearchDemoController {

    @PostMapping({"/document", "/order/index"})
    public ApiResponse<Map<String, Object>> index(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("id", body.getOrDefault("orderId", UUID.randomUUID().toString()),
                "indexed", true, "operation", "index", "document", body));
    }

    @GetMapping({"/document/{id}", "/order/{id}"})
    public ApiResponse<Map<String, Object>> get(@PathVariable String id) {
        return ApiResponse.success(Map.of("id", id, "found", true));
    }

    @GetMapping("/order/search")
    public ApiResponse<Map<String, Object>> search() {
        return ApiResponse.success(Map.of("operation", "search", "results", List.of(Map.of("id", "demo"))));
    }

    @GetMapping("/order/search-page")
    public ApiResponse<Map<String, Object>> searchPage() {
        return ApiResponse.success(Map.of("operation", "searchPage", "results", List.of()));
    }

    @PostMapping("/order/bulk")
    public ApiResponse<Map<String, Object>> bulk(@RequestBody List<Map<String, Object>> body) {
        return ApiResponse.success(Map.of("operation", "bulkIndex", "size", body.size()));
    }

    @PostMapping("/alias/create")
    public ApiResponse<Map<String, Object>> alias(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("operation", "createAlias", "alias", body.get("aliasName")));
    }

    @DeleteMapping("/order/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable String id) {
        return ApiResponse.success(Map.of("id", id, "deleted", true));
    }
}
