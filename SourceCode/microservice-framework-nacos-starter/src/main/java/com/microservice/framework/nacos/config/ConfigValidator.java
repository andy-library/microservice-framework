package com.microservice.framework.nacos.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 配置校验器
 * <p>
 * 提供配置键值对的校验能力，支持必填、数值范围和正则模式校验。
 * 用户可通过注册自定义 {@link ConfigValidator} Bean 来扩展校验逻辑。
 * <p>
 * 内置校验器通过静态工厂方法创建：
 * <ul>
 *   <li>{@link #required()} - 必填校验：确保配置值不为空</li>
 *   <li>{@link #range(long, long)} - 数值范围校验：确保数值在指定范围内</li>
 *   <li>{@link #pattern(String)} - 正则模式校验：确保值匹配指定正则</li>
 * </ul>
 *
 * @author Andy Yang
 */
public interface ConfigValidator {

    /**
     * 校验指定配置键值对
     *
     * @param key   配置键
     * @param value 配置值（可为 null）
     * @return 校验结果
     */
    ValidationResult validate(String key, String value);

    /**
     * 校验结果
     */
    final class ValidationResult {

        private final boolean valid;
        private final List<ValidationIssue> issues;

        private ValidationResult(boolean valid, List<ValidationIssue> issues) {
            this.valid = valid;
            this.issues = Collections.unmodifiableList(issues);
        }

        /**
         * 创建校验通过的结果
         *
         * @return 校验通过的结果
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, Collections.emptyList());
        }

        /**
         * 创建校验失败的结果
         *
         * @param issues 校验问题列表
         * @return 校验失败的结果
         */
        public static ValidationResult invalid(List<ValidationIssue> issues) {
            return new ValidationResult(false, issues);
        }

        /**
         * 创建包含单个校验问题的失败结果
         *
         * @param issue 校验问题
         * @return 校验失败的结果
         */
        public static ValidationResult invalid(ValidationIssue issue) {
            List<ValidationIssue> issues = new ArrayList<>();
            issues.add(issue);
            return new ValidationResult(false, issues);
        }

        public boolean isValid() {
            return valid;
        }

        public List<ValidationIssue> getIssues() {
            return issues;
        }

        @Override
        public String toString() {
            if (valid) {
                return "ValidationResult{valid=true, issues=[]}";
            }
            return "ValidationResult{valid=false, issues=" + issues + '}';
        }
    }

    /**
     * 校验问题
     */
    final class ValidationIssue {

        /**
         * 问题严重级别
         */
        public enum Severity {
            /** 错误级别：阻止应用启动 */
            ERROR,
            /** 警告级别：允许启动但需要关注 */
            WARN
        }

        private final Severity severity;
        private final String key;
        private final String message;

        public ValidationIssue(Severity severity, String key, String message) {
            this.severity = Objects.requireNonNull(severity, "severity must not be null");
            this.key = Objects.requireNonNull(key, "key must not be null");
            this.message = Objects.requireNonNull(message, "message must not be null");
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getKey() {
            return key;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "ValidationIssue{" +
                    "severity=" + severity +
                    ", key='" + key + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    // ========== Built-in validators ==========

    /**
     * 创建必填校验器
     * <p>
     * 确保配置值不为 null 且不为空字符串。
     *
     * @return 必填校验器
     */
    static ConfigValidator required() {
        return new RequiredValidator();
    }

    /**
     * 创建数值范围校验器
     * <p>
     * 确保配置值（解析为 long）在指定的最小值和最大值范围内。
     *
     * @param min 最小值
     * @param max 最大值
     * @return 数值范围校验器
     */
    static ConfigValidator range(long min, long max) {
        return new RangeValidator(min, max);
    }

    /**
     * 创建正则模式校验器
     * <p>
     * 确保配置值匹配指定的正则表达式。
     *
     * @param regex 正则表达式
     * @return 正则模式校验器
     */
    static ConfigValidator pattern(String regex) {
        return new PatternValidator(Pattern.compile(regex));
    }

}
