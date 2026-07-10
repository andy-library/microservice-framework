package com.microservice.demo.controller;

import com.microservice.demo.model.Order;
import com.microservice.demo.service.OrderService;
import com.microservice.framework.logging.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单 Controller
 * 演示：跨服务调用 + Trace 传播
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     * 演示：跨服务调用（调用 UserService）
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        String productName = (String) request.get("productName");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        Log.info("接收创建订单请求")
                .with("userId", userId)
                .with("productName", productName)
                .log();

        Order order = orderService.createOrder(userId, productName, amount);

        return ResponseEntity.ok(order);
    }

    /**
     * 查询订单
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        Order order = orderService.findById(orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable String orderId,
            @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        Order order = orderService.updateStatus(orderId, newStatus);
        return ResponseEntity.ok(order);
    }
}
