package com.microservice.framework.apollo.config;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 正则模式校验器实现
 * <p>
 * 确保配置值匹配指定的正则表达式。
 *
 * @author Andy Yang
 */
final class PatternValidator implements ConfigValidator {

    private final Pattern pattern;

    PatternValidator(Pattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
    }

    @Override
    public ConfigValidator.ValidationResult validate(String key, String value) {
        if (value == null) {
            return ConfigValidator.ValidationResult.valid(); // null is valid for pattern - use required() separately
        }
        if (!pattern.matcher(value).matches()) {
            return ConfigValidator.ValidationResult.invalid(
                    new ConfigValidator.ValidationIssue(ConfigValidator.ValidationIssue.Severity.ERROR, key,
                            "Value '" + value + "' for key '" + key +
                                    "' does not match pattern '" + pattern.pattern() + "'"));
        }
        return ConfigValidator.ValidationResult.valid();
    }

    @Override
    public String toString() {
        return "ConfigValidator.pattern('" + pattern.pattern() + "')";
    }
}
