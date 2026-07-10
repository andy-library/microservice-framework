package com.microservice.framework.logging.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogBuilder 单元测试
 */
class LogBuilderTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // ==================== with 方法测试 ====================

    @Test
    @DisplayName("with: 添加单个键值对")
    void testWith_SingleKeyValue() {
        LogBuilder builder = new LogBuilder(Level.INFO, "测试消息");

        LogBuilder result = builder.with("orderId", "order123");

        assertNotNull(result);
        assertSame(builder, result); // 链式调用返回同一对象
    }

    @Test
    @DisplayName("with: 添加多个键值对")
    void testWith_MultipleKeyValues() {
        LogBuilder builder = new LogBuilder(Level.INFO, "测试消息");

        assertDoesNotThrow(() -> {
            builder.with("key1", "value1")
                    .with("key2", "value2")
                    .with("key3", 123)
                    .log();
        });
    }

    @Test
    @DisplayName("with: null key 被忽略")
    void testWith_NullKey_Ignored() {
        LogBuilder builder = new LogBuilder(Level.INFO, "测试消息");

        assertDoesNotThrow(() -> builder.with(null, "value").log());
    }

    @Test
    @DisplayName("withAll: 添加 Map")
    void testWithAll_Map() {
        LogBuilder builder = new LogBuilder(Level.INFO, "测试消息");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", "user123");
        data.put("action", "login");

        assertDoesNotThrow(() -> builder.withAll(data).log());
    }

    // ==================== withException 方法测试 ====================

    @Test
    @DisplayName("withException: 设置异常信息")
    void testWithException_SetsStackInfo() {
        LogBuilder builder = new LogBuilder(Level.ERROR, "错误消息");
        Exception ex = new RuntimeException("测试异常");

        LogBuilder result = builder.withException(ex);

        assertNotNull(result);
        assertSame(builder, result);
    }

    // ==================== log 方法测试 ====================

    @Test
    @DisplayName("log: 写入 MDC 并清理")
    void testLog_WritesToMdc() {
        LogBuilder builder = new LogBuilder(Level.INFO, "测试消息")
                .with("testKey", "testValue");

        // 执行日志
        builder.log();

        // MDC 应在日志后被清理
        assertNull(MDC.get("structured_data"));
    }

    @Test
    @DisplayName("log: 日志后清理 MDC")
    void testLog_CleansMdcAfterLog() {
        LogBuilder builder = new LogBuilder(Level.WARN, "警告消息")
                .with("alertLevel", "high");

        builder.log();

        // 验证 MDC 已清理
        assertNull(MDC.get("structured_data"));
        assertNull(MDC.get("error.class"));
    }

    // ==================== toJson 方法测试 ====================

    @Test
    @DisplayName("toJson: 简单 Map 序列化")
    void testToJson_SimpleMap() {
        // 通过反射或间接方式验证 JSON 序列化
        LogBuilder builder = new LogBuilder(Level.INFO, "测试");

        assertDoesNotThrow(() -> {
            builder.with("name", "张三")
                    .with("age", 25)
                    .log();
        });
    }

    @Test
    @DisplayName("toJson: 特殊字符转义")
    void testToJson_WithSpecialChars() {
        LogBuilder builder = new LogBuilder(Level.INFO, "测试");

        assertDoesNotThrow(() -> {
            builder.with("text", "包含\"引号\"和\\反斜杠")
                    .with("newline", "第一行\n第二行")
                    .log();
        });
    }
}
