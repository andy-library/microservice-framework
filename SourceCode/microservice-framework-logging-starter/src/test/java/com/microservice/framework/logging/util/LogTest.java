package com.microservice.framework.logging.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Log 工具类单元测试
 */
class LogTest {

    // ==================== 占位符语法测试 ====================

    @Test
    @DisplayName("info: 占位符语法正常工作")
    void testInfo_PlaceholderSyntax() {
        // 不应抛出异常
        assertDoesNotThrow(() -> Log.info("测试消息 orderId={}", "order123"));
    }

    @Test
    @DisplayName("warn: 占位符语法正常工作")
    void testWarn_PlaceholderSyntax() {
        assertDoesNotThrow(() -> Log.warn("警告消息 count={}", 10));
    }

    @Test
    @DisplayName("error: 占位符语法正常工作")
    void testError_PlaceholderSyntax() {
        assertDoesNotThrow(() -> Log.error("错误消息 code={}", 500));
    }

    @Test
    @DisplayName("error: 带异常的日志")
    void testError_WithThrowable() {
        Exception ex = new RuntimeException("测试异常");
        assertDoesNotThrow(() -> Log.error("发生异常", ex));
    }

    // ==================== Fluent API 测试 ====================

    @Test
    @DisplayName("info: Fluent API 返回 LogBuilder")
    void testInfo_FluentApi_ReturnsBuilder() {
        LogBuilder builder = Log.info("Fluent API 测试");

        assertNotNull(builder);
    }

    @Test
    @DisplayName("debug: Fluent API 正常工作")
    void testDebug_FluentApi() {
        LogBuilder builder = Log.debug("Debug 消息");

        assertNotNull(builder);
        assertDoesNotThrow(() -> builder.with("key", "value").log());
    }

    @Test
    @DisplayName("trace: Fluent API 正常工作")
    void testTrace_FluentApi() {
        LogBuilder builder = Log.trace("Trace 消息");

        assertNotNull(builder);
        assertDoesNotThrow(() -> builder.log());
    }

    @Test
    @DisplayName("warn: Fluent API 正常工作")
    void testWarn_FluentApi() {
        LogBuilder builder = Log.warn("Warn 消息");

        assertNotNull(builder);
        assertDoesNotThrow(() -> builder.with("severity", "medium").log());
    }
}
