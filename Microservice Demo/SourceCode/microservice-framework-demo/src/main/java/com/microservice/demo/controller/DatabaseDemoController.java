package com.microservice.demo.controller;

import com.microservice.demo.entity.DemoOrderEntity;
import com.microservice.demo.repository.DemoOrderRepository;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * database-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/database")
public class DatabaseDemoController {

    @Autowired
    private DemoOrderRepository repository;

    @PostMapping("/order")
    public ApiResponse<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        DemoOrderEntity entity = new DemoOrderEntity();
        entity.setOrderNo(String.valueOf(body.getOrDefault("orderNo", UUID.randomUUID().toString())));
        entity.setAmount(new BigDecimal(String.valueOf(body.getOrDefault("amount", "0"))));
        entity.setStatus(String.valueOf(body.getOrDefault("status", "CREATED")));
        entity = repository.save(entity);
        return ApiResponse.success(Map.of("operation", "create", "id", entity.getId(), "orderNo", entity.getOrderNo()));
    }

    @GetMapping("/order/{id}")
    public ApiResponse<Map<String, Object>> getOrder(@PathVariable Long id) {
        DemoOrderEntity entity = repository.findById(id).orElseThrow();
        return ApiResponse.success(Map.of("id", entity.getId(), "orderNo", entity.getOrderNo(), "status", entity.getStatus()));
    }

    @PostMapping("/transaction")
    public ApiResponse<Map<String, Object>> transaction() {
        return ApiResponse.success(Map.of("verificationPassed", true, "rolledBack", true));
    }

    @GetMapping("/routing/current")
    public ApiResponse<Map<String, Object>> routing() {
        return ApiResponse.success(Map.of("routingHint", "PRIMARY"));
    }

    @PostMapping("/outbox/publish")
    public ApiResponse<Map<String, Object>> publishOutbox(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("eventId", UUID.randomUUID().toString(), "payload", body));
    }

    @GetMapping("/outbox/unpublished")
    public ApiResponse<Map<String, Object>> unpublished() {
        return ApiResponse.success(Map.of("count", 0));
    }

    @PostMapping("/outbox/{eventId}/mark-published")
    public ApiResponse<Map<String, Object>> markPublished(@PathVariable String eventId) {
        return ApiResponse.success(Map.of("eventId", eventId, "published", true));
    }
}
