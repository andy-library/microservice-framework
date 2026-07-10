package com.microservice.framework.logging.core.masking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：验证 MaskingRule 枚举的脱敏逻辑
 * 
 * 覆盖场景：
 * 1. MOBILE_PHONE - 手机号脱敏
 * 2. ID_CARD - 身份证号脱敏
 * 3. EMAIL - 邮箱脱敏
 * 4. BANK_CARD - 银行卡号脱敏
 * 5. 枚举属性验证
 */
class MaskingRuleTest {

    // ==================== MOBILE_PHONE 测试 ====================

    @ParameterizedTest
    @CsvSource({
            "13812345678, 138****5678",
            "15912345678, 159****5678",
            "18812345678, 188****5678",
            "19912345678, 199****5678"
    })
    @DisplayName("MOBILE_PHONE: 手机号脱敏正确")
    void testMobilePhone_Masking(String input, String expected) {
        String result = MaskingRule.MOBILE_PHONE.mask(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("MOBILE_PHONE: 正则表达式匹配验证")
    void testMobilePhone_PatternMatching() {
        Pattern pattern = MaskingRule.MOBILE_PHONE.getPattern();

        assertTrue(pattern.matcher("13812345678").find(), "应匹配 138 开头");
        assertTrue(pattern.matcher("15912345678").find(), "应匹配 159 开头");
        assertFalse(pattern.matcher("12812345678").find(), "不应匹配 128 开头");
        assertFalse(pattern.matcher("1381234567").find(), "不应匹配 10 位号码");
        // 注意：find() 会匹配子串，所以 12 位号码中的 11 位子串会被匹配
        // 这是正则的正常行为，在文本中提取手机号时是期望的
    }

    @Test
    @DisplayName("MOBILE_PHONE: 非 11 位输入返回原值")
    void testMobilePhone_InvalidLength() {
        String result = MaskingRule.MOBILE_PHONE.mask("1381234");
        assertEquals("1381234", result, "非 11 位应返回原值");
    }

    // ==================== ID_CARD 测试 ====================

    @Test
    @DisplayName("ID_CARD: 身份证号脱敏正确 (使用 replaceAll)")
    void testIdCard_Masking() {
        // ID_CARD 使用 replaceAll 方式，需要完整匹配后替换
        String input = "110101199001011234";
        String result = MaskingRule.ID_CARD.mask(input);
        assertEquals("110101********1234", result);
    }

    @Test
    @DisplayName("ID_CARD: 正则表达式匹配验证")
    void testIdCard_PatternMatching() {
        Pattern pattern = MaskingRule.ID_CARD.getPattern();

        assertTrue(pattern.matcher("110101199001011234").find(), "应匹配 18 位身份证");
        // 正则 (\\d{6})(\\d{8})(\\d{4}) 匹配 6+8+4=18 位连续数字
        // find() 会在更长的字符串中也能找到匹配
    }

    // ==================== EMAIL 测试 ====================

    @ParameterizedTest
    @CsvSource({
            "example@gmail.com, ex****@gmail.com",
            "test@example.org, te****@example.org",
            "ab@test.com, ab****@test.com"
    })
    @DisplayName("EMAIL: 邮箱脱敏正确")
    void testEmail_Masking(String input, String expected) {
        String result = MaskingRule.EMAIL.mask(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("EMAIL: 正则表达式匹配验证")
    void testEmail_PatternMatching() {
        Pattern pattern = MaskingRule.EMAIL.getPattern();

        assertTrue(pattern.matcher("test@example.com").find(), "应匹配标准邮箱");
        assertTrue(pattern.matcher("test.name@example.com").find(), "应匹配带点号的邮箱");
        assertTrue(pattern.matcher("test_name@example.co.uk").find(), "应匹配带下划线和多级域名的邮箱");
    }

    // ==================== BANK_CARD 测试 ====================

    @ParameterizedTest
    @CsvSource({
            "6222021234567890123, 6222***********0123",
            "6217001234567890, 6217********7890",
            "62170012, 62170012"
    })
    @DisplayName("BANK_CARD: 银行卡号脱敏正确")
    void testBankCard_Masking(String input, String expected) {
        String result = MaskingRule.BANK_CARD.mask(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("BANK_CARD: 短于 8 位的输入返回原值")
    void testBankCard_ShortInput() {
        String result = MaskingRule.BANK_CARD.mask("1234567");
        assertEquals("1234567", result, "短于 8 位应返回原值");
    }

    // ==================== 枚举属性测试 ====================

    @Test
    @DisplayName("枚举属性: getName 返回正确的中文名称")
    void testEnumProperties_Name() {
        assertEquals("手机号", MaskingRule.MOBILE_PHONE.getName());
        assertEquals("身份证号", MaskingRule.ID_CARD.getName());
        assertEquals("邮箱", MaskingRule.EMAIL.getName());
        assertEquals("银行卡号", MaskingRule.BANK_CARD.getName());
    }

    @Test
    @DisplayName("枚举属性: getRegex 返回非空正则")
    void testEnumProperties_Regex() {
        for (MaskingRule rule : MaskingRule.values()) {
            assertNotNull(rule.getRegex(), rule.name() + " 的正则不应为 null");
            assertFalse(rule.getRegex().isEmpty(), rule.name() + " 的正则不应为空");
        }
    }

    @Test
    @DisplayName("枚举属性: getPattern 返回有效的 Pattern 对象")
    void testEnumProperties_Pattern() {
        for (MaskingRule rule : MaskingRule.values()) {
            assertNotNull(rule.getPattern(), rule.name() + " 的 Pattern 不应为 null");
        }
    }

    @Test
    @DisplayName("枚举完整性: 包含所有预期规则")
    void testEnumCompleteness() {
        MaskingRule[] rules = MaskingRule.values();
        assertEquals(4, rules.length, "应有 4 个脱敏规则");

        assertNotNull(MaskingRule.valueOf("MOBILE_PHONE"));
        assertNotNull(MaskingRule.valueOf("ID_CARD"));
        assertNotNull(MaskingRule.valueOf("EMAIL"));
        assertNotNull(MaskingRule.valueOf("BANK_CARD"));
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("边界条件: 空字符串处理")
    void testEdgeCase_EmptyString() {
        for (MaskingRule rule : MaskingRule.values()) {
            String result = rule.mask("");
            assertEquals("", result, rule.name() + " 处理空字符串应返回空字符串");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "这是一段包含手机号13812345678的文本",
            "用户手机: 13812345678, 请联系",
            "13812345678"
    })
    @DisplayName("MOBILE_PHONE: 在文本中正确识别和脱敏")
    void testMobilePhone_InText(String input) {
        Pattern pattern = MaskingRule.MOBILE_PHONE.getPattern();
        assertTrue(pattern.matcher(input).find(), "应能在文本中识别手机号");
    }

    // ==================== 实际脱敏场景测试 ====================

    @Test
    @DisplayName("实际场景: 在文本中脱敏手机号")
    void testMobilePhone_MaskInText() {
        String input = "用户手机13812345678请联系";
        Pattern pattern = MaskingRule.MOBILE_PHONE.getPattern();
        java.util.regex.Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String matched = matcher.group();
            String masked = MaskingRule.MOBILE_PHONE.mask(matched);
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);

        assertEquals("用户手机138****5678请联系", sb.toString());
    }
}
