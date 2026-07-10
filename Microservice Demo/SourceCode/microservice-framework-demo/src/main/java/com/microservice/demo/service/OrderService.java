package com.microservice.demo.service;

import com.microservice.demo.model.Order;
import com.microservice.demo.model.User;
import com.microservice.framework.logging.util.Log;
import com.microservice.framework.observability.annotation.SpanTag;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单服务
 * 演示：
 * 1. 跨服务调用（调用 UserService）
 * 2. Trace 传播
 * 3. Baggage 使用
 */
@Service
public class OrderService {

    @Autowired
    private UserService userService;

    @Autowired
    private Tracer tracer;

    private final Map<String, Order> orderStore = new ConcurrentHashMap<>();

    /**
     * 创建订单
     * 演示跨服务调用和 Trace 传播
     */
    public Order createOrder(@SpanTag("userId") String userId,
            @SpanTag("productName") String productName,
            BigDecimal amount) {

        Log.info("开始创建订单")
                .with("userId", userId)
                .with("productName", productName)
                .with("amount", amount)
                .log();

        // 验证用户存在（跨服务调用）
        User user = userService.findById(userId);

        // 创建订单
        String orderId = UUID.randomUUID().toString();
        Order order = Order.builder()
                .orderId(orderId)
                .userId(userId)
                .productName(productName)
                .amount(amount)
                .status("CREATED")
                .createdAt(System.currentTimeMillis())
                .build();

        orderStore.put(orderId, order);

        // 获取当前 traceId
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "unknown";

        Log.info("订单创建成功")
                .with("orderId", orderId)
                .with("userId", userId)
                .with("traceId", traceId)
                .with("status", "CREATED")
                .log();

        return order;
    }

    /**
     * 查询订单
     */
    public Order findById(@SpanTag("orderId") String orderId) {
        Log.info("查询订单")
                .with("orderId", orderId)
                .log();

        Order order = orderStore.get(orderId);

        if (order == null) {
            Log.warn("订单不存在")
                    .with("orderId", orderId)
                    .log();
            throw new RuntimeException("Order not found: " + orderId);
        }

        return order;
    }

    /**
     * 更新订单状态
     */
    public Order updateStatus(@SpanTag("orderId") String orderId,
            @SpanTag("status") String newStatus) {
        Order order = findById(orderId);
        String oldStatus = order.getStatus();

        order.setStatus(newStatus);

        Log.info("订单状态更新")
                .with("orderId", orderId)
                .with("oldStatus", oldStatus)
                .with("newStatus", newStatus)
                .log();

        return order;
    }
}
