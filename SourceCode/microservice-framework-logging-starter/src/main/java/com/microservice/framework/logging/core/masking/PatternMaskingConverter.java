package com.microservice.framework.logging.core.masking;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * 数据脱敏 Converter
 * 
 * <p>
 * 在日志输出时自动对敏感信息进行脱敏处理。
 * 
 * <h3>设计说明</h3>
 * <ul>
 * <li>支持通过 {@link #setEnabledRules(List)} 配置启用的脱敏规则</li>
 * <li>默认启用所有预设规则（{@link MaskingRule}）</li>
 * <li>使用 volatile 确保多线程可见性</li>
 * <li>使用 EnumSet 优化枚举集合性能</li>
 * </ul>
 * 
 * <h3>使用方式</h3>
 * 
 * <pre>
 * framework:
 *   logging:
 *     masking:
 *       enabled: true
 *       enabled-default-rules:
 *         - MOBILE_PHONE
 *         - ID_CARD
 * </pre>
 * 
 * @author Andy Yang
 * @see MaskingRule
 * @see com.microservice.framework.logging.autoconfigure.MaskingLoggingAutoConfiguration
 */
public class PatternMaskingConverter extends ClassicConverter {

    /**
     * 是否启用脱敏功能
     * 使用 volatile 确保多线程可见性
     */
    private volatile boolean enabled = true;

    /**
     * 启用的脱敏规则集合
     * <p>
     * 使用 EnumSet 而非普通 Set，因为：
     * <ul>
     * <li>EnumSet 使用位向量实现，内存占用小</li>
     * <li>contains/add/remove 等操作都是 O(1) 时间复杂度</li>
     * <li>迭代顺序与枚举定义顺序一致，结果可预测</li>
     * </ul>
     */
    private volatile Set<MaskingRule> enabledRules = EnumSet.allOf(MaskingRule.class);

    /**
     * 将日志消息进行脱敏处理
     * 
     * @param event 日志事件
     * @return 脱敏后的消息；如果未启用或消息为空，返回原始消息
     */
    @Override
    public String convert(ILoggingEvent event) {
        if (!enabled) {
            return event.getFormattedMessage();
        }

        return mask(event.getFormattedMessage());
    }

    /**
     * Masks an arbitrary structured value using the configured rule set.
     *
     * @param message value to mask
     * @return masked value
     */
    public String mask(String message) {
        if (!enabled) {
            return message;
        }
        if (message == null || message.isEmpty()) {
            return message;
        }

        // 仅应用已启用的脱敏规则
        String maskedMessage = message;
        for (MaskingRule rule : enabledRules) {
            maskedMessage = applyMaskingRule(maskedMessage, rule);
        }

        return maskedMessage;
    }

    /**
     * 应用单个脱敏规则
     * 
     * <p>
     * 使用正则表达式匹配敏感信息，并替换为脱敏后的值。
     * 
     * @param message 原始消息
     * @param rule    脱敏规则
     * @return 脱敏后的消息
     */
    private String applyMaskingRule(String message, MaskingRule rule) {
        Matcher matcher = rule.getPattern().matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String matched = matcher.group();
            String masked = rule.mask(matched);
            // 使用 quoteReplacement 防止替换字符串中的特殊字符被解释
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 设置脱敏功能是否启用
     * 
     * @param enabled true 启用，false 禁用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 设置启用的脱敏规则
     * 
     * <p>
     * 根据规则名称列表（如 ["MOBILE_PHONE", "ID_CARD"]）设置启用的规则。
     * 无效的规则名称将被忽略。
     * 
     * <p>
     * 此方法支持 Spring Cloud Config 动态刷新。
     * 
     * @param ruleNames 规则名称列表，对应 {@link MaskingRule} 枚举值名称
     */
    public void setEnabledRules(List<String> ruleNames) {
        if (ruleNames == null || ruleNames.isEmpty()) {
            // 空列表时启用所有规则
            this.enabledRules = EnumSet.allOf(MaskingRule.class);
            return;
        }

        EnumSet<MaskingRule> rules = EnumSet.noneOf(MaskingRule.class);
        for (String name : ruleNames) {
            try {
                MaskingRule rule = MaskingRule.valueOf(name.toUpperCase());
                rules.add(rule);
            } catch (IllegalArgumentException ignored) {
                // 忽略无效的规则名称（容错处理）
            }
        }

        // 如果没有有效规则，则启用所有规则（安全兜底）
        this.enabledRules = rules.isEmpty() ? EnumSet.allOf(MaskingRule.class) : rules;
    }

    /**
     * 获取当前启用的规则集合（只读视图）
     * 
     * @return 不可变的规则集合
     */
    public Set<MaskingRule> getEnabledRules() {
        return Collections.unmodifiableSet(enabledRules);
    }
}
