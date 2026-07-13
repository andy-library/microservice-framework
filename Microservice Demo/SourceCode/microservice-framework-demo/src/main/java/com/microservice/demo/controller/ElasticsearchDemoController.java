package com.microservice.demo.controller;

import com.microservice.framework.elasticsearch.api.ElasticsearchOperations;
import com.microservice.framework.elasticsearch.api.IndexManager;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** elasticsearch-starter public capability endpoints. @author Andy Yang */
@RestController
@RequestMapping({"/demo/elasticsearch", "/demo/es"})
public class ElasticsearchDemoController {
    private static final String INDEX = "demo-orders";
    private final ElasticsearchOperations operations;
    private final IndexManager indexManager;

    public ElasticsearchDemoController(ElasticsearchOperations operations, IndexManager indexManager) {
        this.operations = operations;
        this.indexManager = indexManager;
    }

    @PostMapping({"/document", "/order/index"})
    public ApiResponse<Map<String, Object>> index(@RequestBody Map<String, Object> body) {
        String id = String.valueOf(body.getOrDefault("orderId", UUID.randomUUID().toString()));
        return ApiResponse.success(Map.of("id", operations.index(INDEX, body, id), "indexed", true,
                "operation", "index", "document", body));
    }

    @GetMapping({"/document/{id}", "/order/{id}"})
    public ApiResponse<Map<String, Object>> get(@PathVariable String id) {
        Object document = operations.get(INDEX, id, Map.class).orElse(Map.of());
        return ApiResponse.success(Map.of("id", id, "found", !Map.of().equals(document), "document", document));
    }

    @GetMapping("/order/search")
    public ApiResponse<Map<String, Object>> search(@RequestParam(defaultValue = "status") String field,
                                                    @RequestParam(defaultValue = "CREATED") String value) {
        List<Map> results = operations.search(INDEX, SearchQueryBuilder.create().match(field, value), Map.class);
        return ApiResponse.success(Map.of("operation", "search", "results", results, "total", results.size()));
    }

    @GetMapping("/order/search-page")
    public ApiResponse<Map<String, Object>> searchPage(@RequestParam(defaultValue = "status") String field,
                                                        @RequestParam(defaultValue = "CREATED") String value) {
        List<Map> results = operations.search(INDEX, SearchQueryBuilder.create().term(field, value), PageRequest.of(0, 10), Map.class);
        return ApiResponse.success(Map.of("operation", "searchPage", "results", results, "total", results.size()));
    }

    @PostMapping("/order/bulk")
    public ApiResponse<Map<String, Object>> bulk(@RequestBody List<Map<String, Object>> body) {
        ElasticsearchOperations.BulkResult result = operations.bulkIndex(INDEX, body);
        return ApiResponse.success(Map.of("operation", "bulkIndex", "size", body.size(),
                "successfulIds", result.successfulIds(), "failedIds", result.failedIds(),
                "failedItems", result.failedItems()));
    }

    @PostMapping("/alias/create")
    public ApiResponse<Map<String, Object>> alias(@RequestBody Map<String, Object> body) {
        String alias = String.valueOf(body.get("aliasName"));
        return ApiResponse.success(Map.of("operation", "createAlias", "alias", alias,
                "created", indexManager.createAlias(INDEX, alias)));
    }

    @DeleteMapping("/order/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable String id) {
        String deletedId = operations.delete(INDEX, id);
        return ApiResponse.success(Map.of("id", id, "deletedId", deletedId == null ? "" : deletedId,
                "deleted", deletedId != null));
    }
}
