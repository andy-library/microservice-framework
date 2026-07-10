package com.microservice.framework.logging.core.masking;

import java.util.regex.Pattern;

/**
 * 预设脱敏规则
 * 
 * @author Andy Yang
 */
public enum MaskingRule {

    /**
     * 手机号脱敏：保留前3位和后4位
     * 例如：13812345678 -> 138****5678
     */
    MOBILE_PHONE("手机号", "1[3-9]\\d{9}", input -> {
        if (input.length() == 11) {
            return input.substring(0, 3) + "****" + input.substring(7);
        }
        return input;
    }),

    /**
     * 身份证号脱敏：保留前6位和后4位
     * 例如：110101199001011234 -> 110101********1234
     */
    ID_CARD("身份证号", "(\\d{6})(\\d{8})(\\d{4})", "$1********$3"),

    /**
     * 邮箱脱敏：保留前2位和@后的域名
     * 例如：example@gmail.com -> ex****@gmail.com
     */
    EMAIL("邮箱", "([\\w.]{0,2})[\\w.]*@([\\w.]+)", "$1****@$2"),

    /**
     * 银行卡号脱敏：保留前4位和后4位
     * 例如：6222021234567890123 -> 6222************0123
     */
    BANK_CARD("银行卡号", "(\\d{4})(\\d+)(\\d{4})", input -> {
        if (input.length() >= 8) {
            String prefix = input.substring(0, 4);
            String suffix = input.substring(input.length() - 4);
            int maskLength = input.length() - 8;
            return prefix + "*".repeat(maskLength) + suffix;
        }
        return input;
    });

    private final String name;
    private final String regex;
    private final Pattern pattern;
    private final MaskingFunction maskingFunction;

    MaskingRule(String name, String regex, String replacement) {
        this.name = name;
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
        this.maskingFunction = input -> input.replaceAll(regex, replacement);
    }

    MaskingRule(String name, String regex, MaskingFunction maskingFunction) {
        this.name = name;
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
        this.maskingFunction = maskingFunction;
    }

    public String getName() {
        return name;
    }

    public String getRegex() {
        return regex;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String mask(String input) {
        return maskingFunction.apply(input);
    }

    @FunctionalInterface
    interface MaskingFunction {
        String apply(String input);
    }
}
