package com.microservice.framework.nacos.config;

import java.util.Objects;

/**
 * 数值范围校验器实现
 * <p>
 * 确保配置值（解析为 long）在指定的最小值和最大值范围内。
 *
 * @author Andy Yang
 */
final class RangeValidator implements ConfigValidator {

    private final long min;
    private final long max;

    RangeValidator(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException(
                    "min must not be greater than max: min=" + min + ", max=" + max);
        }
        this.min = min;
        this.max = max;
    }

    @Override
    public ConfigValidator.ValidationResult validate(String key, String value) {
        if (value == null || value.isBlank()) {
            return ConfigValidator.ValidationResult.valid(); // null is valid for range - use required() separately
        }
        try {
            long numericValue = Long.parseLong(value);
            if (numericValue < min || numericValue > max) {
                return ConfigValidator.ValidationResult.invalid(
                        new ConfigValidator.ValidationIssue(ConfigValidator.ValidationIssue.Severity.ERROR, key,
                                "Value '" + value + "' for key '" + key +
                                        "' is out of range [" + min + ", " + max + "]"));
            }
            return ConfigValidator.ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ConfigValidator.ValidationResult.invalid(
                    new ConfigValidator.ValidationIssue(ConfigValidator.ValidationIssue.Severity.ERROR, key,
                            "Value '" + value + "' for key '" + key + "' is not a valid number"));
        }
    }

    @Override
    public String toString() {
        return "ConfigValidator.range(" + min + ", " + max + ")";
    }
}
