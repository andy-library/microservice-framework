package com.microservice.demo.controller;

import com.microservice.framework.logging.util.Log;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 指标演示 Controller
 * 演示自定义业务指标的埋点
 */
@RestController
@RequestMapping("/api/demo/metrics")
public class MetricsDemoController {

    private final MeterRegistry meterRegistry;
    private final Counter orderCreateCounter;
    private final Timer paymentProcessTimer;

    public MetricsDemoController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 1. 定义计数器：统计订单创建次数
        this.orderCreateCounter = Counter.builder("business.order.created")
                .description("Total number of orders created")
                .tag("region", "cn-north") // 静态标签
                .register(meterRegistry);

        // 2. 定义计时器：统计支付处理耗时
        this.paymentProcessTimer = Timer.builder("business.payment.process")
                .description("Time taken to process payment")
                .publishPercentiles(0.5, 0.95, 0.99) // 发布 P50, P95, P99 分位数
                .register(meterRegistry);
    }

    /**
     * 演示计数器 (Counter)
     */
    @GetMapping("/counter")
    public ResponseEntity<Map<String, Object>> incrementCounter(@RequestParam(defaultValue = "mobile") String channel) {
        // 增加计数，并带上动态标签
        // 注意：标签值必须是有限基数的枚举值，不能是 userId 等无限值
        meterRegistry.counter("business.order.created", "channel", channel).increment();

        // 同时增加总计数器
        orderCreateCounter.increment();

        Log.info("业务指标埋点：计数器+1")
                .with("metric", "business.order.created")
                .with("channel", channel)
                .log();

        return ResponseEntity.ok(Map.of(
                "message", "Counter incremented",
                "metric", "business.order.created",
                "tags", Map.of("channel", channel)));
    }

    /**
     * 演示计时器 (Timer)
     */
    @GetMapping("/timer")
    public ResponseEntity<Map<String, Object>> recordTimer() {
        long startTime = System.currentTimeMillis();

        // 模拟业务耗时
        paymentProcessTimer.record(() -> {
            try {
                // 随机休眠 10-100ms
                Thread.sleep((long) (Math.random() * 90 + 10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        long duration = System.currentTimeMillis() - startTime;

        Log.info("业务指标埋点：计时器记录")
                .with("metric", "business.payment.process")
                .with("durationMs", duration)
                .log();

        return ResponseEntity.ok(Map.of(
                "message", "Timer recorded",
                "metric", "business.payment.process",
                "actualDurationMs", duration));
    }
}
