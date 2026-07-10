package com.microservice.framework.logging.core.masking;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：验证 PatternMaskingConverter 的脱敏转换功能
 *
 * <p>使用手写 FakeLoggingEvent 替代 Mockito mock ILoggingEvent，
 * 避免 ByteBuddy inline mock maker 初始化失败。
 *
 * 覆盖场景：
 * 1. 启用/禁用脱敏功能
 * 2. 启用特定规则
 * 3. 空消息和 null 消息处理
 * 4. 多种敏感数据混合脱敏
 * 5. 规则配置动态更新
 */
class PatternMaskingConverterTest {

    private PatternMaskingConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PatternMaskingConverter();
    }

    /**
     * Creates a FakeLoggingEvent using Proxy that returns the given formatted message.
     * Only getFormattedMessage() is used by PatternMaskingConverter.
     */
    private ILoggingEvent eventWithMessage(String message) {
        return (ILoggingEvent) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ILoggingEvent.class},
                (proxy, method, args) -> {
                    if ("getFormattedMessage".equals(method.getName())) return message;
                    if ("getMessage".equals(method.getName())) return message;
                    return null;
                });
    }

    // ==================== 启用/禁用测试 ====================

    @Test
    @DisplayName("禁用状态: 返回原始消息")
    void testDisabled_ReturnsOriginalMessage() {
        converter.setEnabled(false);
        String originalMessage = "用户手机: 13812345678";

        String result = converter.convert(eventWithMessage(originalMessage));

        assertEquals(originalMessage, result, "禁用时应返回原始消息");
    }

    @Test
    @DisplayName("启用状态: 脱敏手机号")
    void testEnabled_MasksMobilePhone() {
        converter.setEnabled(true);
        String message = "手机: 13812345678";

        String result = converter.convert(eventWithMessage(message));

        assertTrue(result.contains("138****5678"), "应脱敏手机号");
    }

    // ==================== 规则配置测试 ====================

    @Test
    @DisplayName("规则配置: 只启用手机号规则")
    void testRuleConfig_OnlyMobilePhone() {
        converter.setEnabled(true);
        converter.setEnabledRules(Arrays.asList("MOBILE_PHONE"));

        String message = "手机: 13812345678";

        String result = converter.convert(eventWithMessage(message));

        assertTrue(result.contains("138****5678"), "应脱敏手机号");
    }

    @Test
    @DisplayName("规则配置: 启用多个规则")
    void testRuleConfig_MultipleRules() {
        converter.setEnabled(true);
        converter.setEnabledRules(Arrays.asList("MOBILE_PHONE", "EMAIL"));

        String message = "手机: 13812345678, 邮箱: test@gmail.com";

        String result = converter.convert(eventWithMessage(message));

        assertTrue(result.contains("138****5678"), "应脱敏手机号");
        assertTrue(result.contains("te****@gmail.com"), "应脱敏邮箱");
    }

    @Test
    @DisplayName("规则配置: 空列表时启用所有规则")
    void testRuleConfig_EmptyListEnablesAll() {
        converter.setEnabled(true);
        converter.setEnabledRules(Collections.emptyList());

        Set<MaskingRule> enabledRules = converter.getEnabledRules();
        assertEquals(MaskingRule.values().length, enabledRules.size(), "空列表应启用所有规则");
    }

    @Test
    @DisplayName("规则配置: null 列表时启用所有规则")
    void testRuleConfig_NullListEnablesAll() {
        converter.setEnabled(true);
        converter.setEnabledRules(null);

        Set<MaskingRule> enabledRules = converter.getEnabledRules();
        assertEquals(MaskingRule.values().length, enabledRules.size(), "null 列表应启用所有规则");
    }

    @Test
    @DisplayName("规则配置: 忽略无效规则名称")
    void testRuleConfig_IgnoreInvalidRuleNames() {
        converter.setEnabled(true);
        converter.setEnabledRules(Arrays.asList("MOBILE_PHONE", "INVALID_RULE", "UNKNOWN"));

        Set<MaskingRule> enabledRules = converter.getEnabledRules();
        assertEquals(1, enabledRules.size(), "应只有一个有效规则");
        assertTrue(enabledRules.contains(MaskingRule.MOBILE_PHONE));
    }

    @Test
    @DisplayName("规则配置: 全部无效规则时启用所有规则（安全兜底）")
    void testRuleConfig_AllInvalidEnablesAll() {
        converter.setEnabled(true);
        converter.setEnabledRules(Arrays.asList("INVALID1", "INVALID2"));

        Set<MaskingRule> enabledRules = converter.getEnabledRules();
        assertEquals(MaskingRule.values().length, enabledRules.size(), "全部无效时应启用所有规则");
    }

    @Test
    @DisplayName("规则配置: 支持小写规则名称")
    void testRuleConfig_CaseInsensitive() {
        converter.setEnabled(true);
        converter.setEnabledRules(Arrays.asList("mobile_phone", "email"));

        Set<MaskingRule> enabledRules = converter.getEnabledRules();
        assertEquals(2, enabledRules.size());
        assertTrue(enabledRules.contains(MaskingRule.MOBILE_PHONE));
        assertTrue(enabledRules.contains(MaskingRule.EMAIL));
    }

    // ==================== 空消息处理测试 ====================

    @Test
    @DisplayName("空消息: null 消息返回 null")
    void testNullMessage_ReturnsNull() {
        converter.setEnabled(true);

        String result = converter.convert(eventWithMessage(null));

        assertNull(result);
    }

    @Test
    @DisplayName("空消息: 空字符串返回空字符串")
    void testEmptyMessage_ReturnsEmpty() {
        converter.setEnabled(true);

        String result = converter.convert(eventWithMessage(""));

        assertEquals("", result);
    }

    @Test
    @DisplayName("无敏感数据: 无需脱敏时返回原消息")
    void testNoSensitiveData_ReturnsOriginal() {
        converter.setEnabled(true);
        String message = "这是一条普通日志消息，没有敏感数据";

        String result = converter.convert(eventWithMessage(message));

        assertEquals(message, result);
    }

    // ==================== 邮箱脱敏测试 ====================

    @Test
    @DisplayName("邮箱脱敏: 正确脱敏邮箱地址")
    void testEmail_Masking() {
        converter.setEnabled(true);
        converter.setEnabledRules(Arrays.asList("EMAIL"));
        String message = "用户邮箱: test@example.com";

        String result = converter.convert(eventWithMessage(message));

        assertTrue(result.contains("te****@example.com"), "应脱敏邮箱: " + result);
    }

    // ==================== 规则集合只读性测试 ====================

    @Test
    @DisplayName("规则集合: getEnabledRules 返回不可变集合")
    void testGetEnabledRules_Unmodifiable() {
        Set<MaskingRule> rules = converter.getEnabledRules();

        assertThrows(UnsupportedOperationException.class, () -> {
            rules.add(MaskingRule.MOBILE_PHONE);
        }, "返回的规则集合应是不可变的");
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("边界条件: 单个敏感数据")
    void testSingleSensitiveData() {
        converter.setEnabled(true);
        converter.setEnabledRules(Arrays.asList("MOBILE_PHONE"));

        String result = converter.convert(eventWithMessage("13812345678"));

        assertEquals("138****5678", result);
    }

    @Test
    @DisplayName("边界条件: 重复的敏感数据")
    void testDuplicateSensitiveData() {
        converter.setEnabled(true);
        converter.setEnabledRules(Arrays.asList("MOBILE_PHONE"));
        String message = "手机1: 13812345678, 手机2: 13812345678";

        String result = converter.convert(eventWithMessage(message));

        assertEquals("手机1: 138****5678, 手机2: 138****5678", result);
    }

    @Test
    @DisplayName("边界条件: 特殊字符在消息中")
    void testSpecialCharactersInMessage() {
        converter.setEnabled(true);
        converter.setEnabledRules(Arrays.asList("MOBILE_PHONE"));
        String message = "手机号$13812345678$请联系";

        String result = converter.convert(eventWithMessage(message));

        assertNotNull(result);
        assertTrue(result.contains("138****5678"));
    }

    @Test
    @DisplayName("边界条件: 只禁用某些规则后验证")
    void testDisableSpecificRule() {
        converter.setEnabled(true);
        // 只启用 EMAIL，不启用 MOBILE_PHONE
        converter.setEnabledRules(Arrays.asList("EMAIL"));

        String message = "手机: 13812345678, 邮箱: test@example.com";

        String result = converter.convert(eventWithMessage(message));

        // 手机号不应被脱敏
        assertTrue(result.contains("13812345678"), "手机号不应被脱敏");
        // 邮箱应被脱敏
        assertTrue(result.contains("te****@example.com"), "邮箱应被脱敏");
    }

}
