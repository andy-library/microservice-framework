package com.microservice.framework.logging.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：验证 LoggingProperties 的默认配置值
 * 确保所有默认值符合 PRD 文档定义
 */
class LoggingPropertiesTest {

    /**
     * 场景 1: 验证 Async 默认配置
     */
    @Test
    @DisplayName("Async 默认配置: enabled=true, queueSize=256, discardingThreshold=0")
    void testAsyncDefaultValues() {
        LoggingProperties.AsyncProperties async = new LoggingProperties.AsyncProperties();

        assertTrue(async.isEnabled(), "异步日志默认应启用");
        assertEquals(256, async.getQueueSize(), "默认队列大小应为 256");
        assertEquals(0, async.getDiscardingThreshold(), "默认丢弃阈值应为 0 (不丢弃)");
    }

    @Test
    @DisplayName("Logback 默认配置应与 LoggingProperties 异步默认值一致")
    void logbackDefaultShouldMatchAsyncPropertiesDefaults() throws Exception {
        LoggingProperties.AsyncProperties async = new LoggingProperties.AsyncProperties();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("logback-spring.xml")) {
            assertNotNull(inputStream, "logback-spring.xml must exist in starter resources");
            String logback = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(logback.contains("source=\"framework.logging.async.queue-size\" defaultValue=\"" + async.getQueueSize() + "\""),
                    "logback queue-size default must match LoggingProperties");
            assertTrue(logback.contains("source=\"framework.logging.async.discarding-threshold\" defaultValue=\""
                            + async.getDiscardingThreshold() + "\""),
                    "logback discarding-threshold default must match LoggingProperties");
            assertTrue(logback.contains("source=\"framework.logging.async.enabled\" defaultValue=\"" + async.isEnabled() + "\""),
                    "logback must bind async enabled property");
            assertTrue(logback.contains("ASYNC_ENABLED"),
                    "logback must use async enabled property to choose async or direct appender");
        }
    }

    /**
     * 场景 2: 验证 FloodProtection 默认配置
     */
    @Test
    @DisplayName("FloodProtection 默认配置: enabled=true, rate=10, burstCapacity=100")
    void testFloodProtectionDefaultValues() {
        LoggingProperties.FloodProtectionProperties floodProtection = new LoggingProperties.FloodProtectionProperties();

        assertTrue(floodProtection.isEnabled(), "日志风暴防护默认应启用");
        assertEquals(10, floodProtection.getRate(), "默认限流速率应为 10 QPS");
        assertEquals(100, floodProtection.getBurstCapacity(), "默认突发容量应为 100");
    }

    /**
     * 场景 3: 验证 Masking 默认配置
     */
    @Test
    @DisplayName("Masking 默认配置: enabled=false, 默认规则含 MOBILE_PHONE 和 ID_CARD")
    void testMaskingDefaultValues() {
        LoggingProperties.MaskingProperties masking = new LoggingProperties.MaskingProperties();

        assertFalse(masking.isEnabled(), "数据脱敏默认应禁用");
        assertNotNull(masking.getEnabledDefaultRules(), "默认规则列表不应为 null");
        assertEquals(2, masking.getEnabledDefaultRules().size(), "默认规则列表应包含 2 项");
        assertTrue(masking.getEnabledDefaultRules().contains("MOBILE_PHONE"), "默认规则应包含 MOBILE_PHONE");
        assertTrue(masking.getEnabledDefaultRules().contains("ID_CARD"), "默认规则应包含 ID_CARD");
        assertNotNull(masking.getCustomRules(), "自定义规则列表不应为 null");
        assertTrue(masking.getCustomRules().isEmpty(), "自定义规则列表默认应为空");
    }

    /**
     * 场景 4: 验证 TraceSampling 默认配置
     */
    @Test
    @DisplayName("TraceSampling 默认配置: enabled=true, levelForUnsampled=INFO")
    void testTraceSamplingDefaultValues() {
        LoggingProperties.TraceSamplingProperties traceSampling = new LoggingProperties.TraceSamplingProperties();

        assertTrue(traceSampling.isEnabled(), "Trace 关联采样默认应启用");
        assertEquals("INFO", traceSampling.getLevelForUnsampled(), "未采样 Trace 的日志级别阈值默认应为 INFO");
    }

    /**
     * 场景 5: 验证 LoggingProperties 整体默认配置
     */
    @Test
    @DisplayName("LoggingProperties 整体默认配置验证")
    void testLoggingPropertiesDefaultValues() {
        LoggingProperties props = new LoggingProperties();

        assertNotNull(props.getAsync(), "Async 配置不应为 null");
        assertNotNull(props.getFloodProtection(), "FloodProtection 配置不应为 null");
        assertNotNull(props.getMasking(), "Masking 配置不应为 null");
        assertNotNull(props.getTraceSampling(), "TraceSampling 配置不应为 null");
    }

    /**
     * 场景 6: 验证 CustomMaskingRule 属性设置
     */
    @Test
    @DisplayName("CustomMaskingRule 属性设置验证")
    void testCustomMaskingRuleProperties() {
        LoggingProperties.CustomMaskingRule rule = new LoggingProperties.CustomMaskingRule();

        rule.setName("EMAIL");
        rule.setRegex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        rule.setMask("****@****");

        assertEquals("EMAIL", rule.getName());
        assertNotNull(rule.getRegex());
        assertEquals("****@****", rule.getMask());
    }
}
