package com.microservice.framework.logging.core.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：验证 RateLimitingTurboFilter 的令牌桶限流逻辑
 * 
 * 覆盖场景：
 * 1. 禁用状态时放行所有日志
 * 2. 启用状态时正常限流
 * 3. 突发容量测试
 * 4. 令牌补充测试
 * 5. 配置变更测试
 * 6. 并发安全测试
 * 
 * 注意：不使用 Mockito mock Marker/Logger 接口，避免 Java 24+ 兼容性问题
 */
class RateLimitingTurboFilterTest {

    private RateLimitingTurboFilter filter;
    private Logger testLogger;
    private AtomicLong nanoTime;

    @BeforeEach
    void setUp() {
        nanoTime = new AtomicLong();
        filter = new RateLimitingTurboFilter(nanoTime::get);
        filter.setRate(10); // 每秒 10 个令牌
        filter.setBurstCapacity(100); // 最大容量 100
        filter.setEnabled(true);
        testLogger = (Logger) LoggerFactory.getLogger(RateLimitingTurboFilterTest.class);
    }

    // ==================== 基本功能测试 ====================

    @Test
    @DisplayName("禁用状态: 所有日志都放行")
    void testDisabled_AllLogsPassed() {
        filter.setEnabled(false);

        for (int i = 0; i < 200; i++) {
            FilterReply reply = filter.decide(null, testLogger, Level.INFO, "test message", null, null);
            assertEquals(FilterReply.NEUTRAL, reply, "禁用状态下所有日志应放行");
        }
    }

    @Test
    @DisplayName("启用状态: 默认配置验证")
    void testDefaultConfig() {
        assertEquals(10, filter.getRate(), "默认速率应为 10");
        assertEquals(100, filter.getBurstCapacity(), "默认突发容量应为 100");
        assertTrue(filter.isEnabled(), "默认应启用");
    }

    // ==================== 令牌桶算法测试 ====================

    @Test
    @DisplayName("突发容量: 初始应能处理 burstCapacity 数量的日志")
    void testBurstCapacity_InitialBurst() {
        int passedCount = 0;
        for (int i = 0; i < 150; i++) {
            FilterReply reply = filter.decide(null, testLogger, Level.INFO, "test", null, null);
            if (reply == FilterReply.NEUTRAL) {
                passedCount++;
            }
        }

        // 应该有大约 burstCapacity 个通过（可能略有误差）
        assertTrue(passedCount >= 95 && passedCount <= 105,
                "应该有约 100 个日志通过，实际: " + passedCount);
    }

    @Test
    @DisplayName("限流效果: 超过容量的日志应被拒绝")
    void testRateLimiting_ExcessLogsRejected() {
        int passedCount = 0;
        int rejectedCount = 0;

        // 快速发送大量日志
        for (int i = 0; i < 200; i++) {
            FilterReply reply = filter.decide(null, testLogger, Level.INFO, "test", null, null);
            if (reply == FilterReply.NEUTRAL) {
                passedCount++;
            } else {
                rejectedCount++;
            }
        }

        assertTrue(rejectedCount > 0, "应有部分日志被拒绝");
        assertTrue(passedCount < 200, "不应所有日志都通过");
    }

    @Test
    @DisplayName("令牌补充: 等待后应有新令牌可用")
    void testTokenRefill() {
        // 消耗所有令牌
        for (int i = 0; i < 150; i++) {
            filter.decide(null, testLogger, Level.INFO, "test", null, null);
        }

        // 验证令牌耗尽
        FilterReply exhaustedReply = filter.decide(null, testLogger, Level.INFO, "test", null, null);
        assertEquals(FilterReply.DENY, exhaustedReply, "令牌耗尽后应拒绝");

        // 等待令牌补充（1 秒应补充约 10 个令牌）
        advanceMillis(1100);

        // 验证有新令牌可用
        FilterReply refreshedReply = filter.decide(null, testLogger, Level.INFO, "test", null, null);
        assertEquals(FilterReply.NEUTRAL, refreshedReply, "补充后应有令牌可用");
    }

    // ==================== 配置变更测试 ====================

    @Test
    @DisplayName("配置变更: 动态修改速率")
    void testConfigChange_Rate() {
        filter.setRate(20);
        assertEquals(20, filter.getRate());
    }

    @Test
    @DisplayName("配置变更: 动态修改突发容量（容量缩小时重置令牌）")
    void testConfigChange_BurstCapacity_Shrink() {
        // 缩小容量
        filter.setBurstCapacity(50);

        // 验证容量
        assertEquals(50, filter.getBurstCapacity());

        // 消耗 60 个，应该只有约 50 个能通过
        int passedCount = 0;
        for (int i = 0; i < 60; i++) {
            FilterReply reply = filter.decide(null, testLogger, Level.INFO, "test", null, null);
            if (reply == FilterReply.NEUTRAL) {
                passedCount++;
            }
        }

        assertTrue(passedCount <= 55, "缩小容量后不应超过新容量");
    }

    @Test
    @DisplayName("配置变更: 动态启用/禁用")
    void testConfigChange_Enable() {
        filter.setEnabled(false);
        FilterReply disabledReply = filter.decide(null, testLogger, Level.INFO, "test", null, null);
        assertEquals(FilterReply.NEUTRAL, disabledReply);

        filter.setEnabled(true);
        // 启用后仍应工作正常
        FilterReply enabledReply = filter.decide(null, testLogger, Level.INFO, "test", null, null);
        assertNotNull(enabledReply);
    }

    // ==================== 并发安全测试 ====================

    @Test
    @DisplayName("并发安全: 多线程同时消费令牌")
    void testConcurrency_MultipleThreads() throws InterruptedException {
        filter.setBurstCapacity(100);
        filter.setRate(0); // 禁止补充，便于测试

        int threadCount = 10;
        int requestsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalPassed = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < requestsPerThread; i++) {
                        FilterReply reply = filter.decide(null, testLogger, Level.INFO, "test", null, null);
                        if (reply == FilterReply.NEUTRAL) {
                            totalPassed.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 总共应该有大约 burstCapacity 个通过（由于 rate=0，不会补充新令牌）
        assertTrue(totalPassed.get() <= 110,
                "并发情况下不应超过容量，实际: " + totalPassed.get());
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("边界条件: rate=0 时不补充令牌")
    void testEdgeCase_ZeroRate() {
        filter.setRate(0);
        filter.setBurstCapacity(10);

        // 先消耗完令牌
        for (int i = 0; i < 15; i++) {
            filter.decide(null, testLogger, Level.INFO, "test", null, null);
        }

        // 推进时间后仍不应补充令牌
        advanceMillis(1000);

        // 应该仍然没有令牌
        FilterReply reply = filter.decide(null, testLogger, Level.INFO, "test", null, null);
        assertEquals(FilterReply.DENY, reply, "rate=0 时不应补充令牌");
    }

    @Test
    @DisplayName("边界条件: burstCapacity=1 时只允许一条日志")
    void testEdgeCase_MinimalBurstCapacity() {
        filter.setBurstCapacity(1);
        filter.setRate(1);

        FilterReply first = filter.decide(null, testLogger, Level.INFO, "first", null, null);
        assertEquals(FilterReply.NEUTRAL, first, "第一条应通过");

        FilterReply second = filter.decide(null, testLogger, Level.INFO, "second", null, null);
        assertEquals(FilterReply.DENY, second, "第二条应被拒绝");
    }

    @Test
    @DisplayName("边界条件: 所有日志级别都应用限流")
    void testAllLogLevels() throws InterruptedException {
        Level[] levels = { Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR };

        // 先消耗完令牌
        for (int i = 0; i < 150; i++) {
            filter.decide(null, testLogger, Level.INFO, "test", null, null);
        }

        for (Level level : levels) {
            FilterReply reply = filter.decide(null, testLogger, level, "test", null, null);
            assertEquals(FilterReply.DENY, reply, "所有级别的日志都应被限流: " + level);
        }
    }

    // ==================== Getter 测试 ====================

    @Test
    @DisplayName("Getter: isEnabled 返回正确值")
    void testIsEnabled() {
        assertTrue(filter.isEnabled());
        filter.setEnabled(false);
        assertFalse(filter.isEnabled());
    }

    private void advanceMillis(long millis) {
        nanoTime.addAndGet(millis * 1_000_000L);
    }

    @Test
    @DisplayName("Getter: getRate 返回正确值")
    void testGetRate() {
        assertEquals(10, filter.getRate());
        filter.setRate(50);
        assertEquals(50, filter.getRate());
    }

    @Test
    @DisplayName("Getter: getBurstCapacity 返回正确值")
    void testGetBurstCapacity() {
        assertEquals(100, filter.getBurstCapacity());
        filter.setBurstCapacity(200);
        assertEquals(200, filter.getBurstCapacity());
    }
}
