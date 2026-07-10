package com.microservice.framework.nacos.config;

/**
 * 必填校验器实现
 * <p>
 * 确保配置值不为 null 且不为空字符串。
 *
 * @author Andy Yang
 */
final class RequiredValidator implements ConfigValidator {

    @Override
    public ConfigValidator.ValidationResult validate(String key, String value) {
        if (value == null || value.isBlank()) {
            return ConfigValidator.ValidationResult.invalid(
                    new ConfigValidator.ValidationIssue(ConfigValidator.ValidationIssue.Severity.ERROR, key,
                            "Required configuration key '" + key + "' has no value"));
        }
        return ConfigValidator.ValidationResult.valid();
    }

    @Override
    public String toString() {
        return "ConfigValidator.required()";
    }
}
