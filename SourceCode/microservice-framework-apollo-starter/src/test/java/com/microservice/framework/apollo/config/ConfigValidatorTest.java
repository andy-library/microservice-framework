package com.microservice.framework.apollo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ConfigValidator 测试
 * <p>
 * 验证必填校验器、数值范围校验器和正则模式校验器的校验逻辑。
 *
 * @author Andy Yang
 */
class ConfigValidatorTest {

    @Nested
    @DisplayName("必填校验器 required()")
    class RequiredValidatorTests {

        private final ConfigValidator validator = ConfigValidator.required();

        @Test
        @DisplayName("非空值应校验通过")
        void nonEmptyValueShouldBeValid() {
            ConfigValidator.ValidationResult result = validator.validate("db.url", "jdbc:mysql://localhost");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getIssues()).isEmpty();
        }

        @Test
        @DisplayName("null 值应校验失败")
        void nullValueShouldBeInvalid() {
            ConfigValidator.ValidationResult result = validator.validate("db.url", null);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues()).hasSize(1);
            assertThat(result.getIssues().get(0).getSeverity())
                    .isEqualTo(ConfigValidator.ValidationIssue.Severity.ERROR);
            assertThat(result.getIssues().get(0).getKey()).isEqualTo("db.url");
            assertThat(result.getIssues().get(0).getMessage()).contains("db.url");
        }

        @Test
        @DisplayName("空字符串应校验失败")
        void blankValueShouldBeInvalid() {
            ConfigValidator.ValidationResult result = validator.validate("db.url", "   ");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues()).hasSize(1);
        }

        @Test
        @DisplayName("空字符串值应校验失败")
        void emptyValueShouldBeInvalid() {
            ConfigValidator.ValidationResult result = validator.validate("db.url", "");

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("toString 应返回 ConfigValidator.required()")
        void toStringShouldReturnReadableName() {
            assertThat(validator.toString()).isEqualTo("ConfigValidator.required()");
        }
    }

    @Nested
    @DisplayName("数值范围校验器 range()")
    class RangeValidatorTests {

        private final ConfigValidator validator = ConfigValidator.range(0, 100);

        @Test
        @DisplayName("范围内的值应校验通过")
        void valueInRangeShouldBeValid() {
            ConfigValidator.ValidationResult result = validator.validate("pool.size", "50");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getIssues()).isEmpty();
        }

        @Test
        @DisplayName("最小边界值应校验通过")
        void minValueShouldBeValid() {
            ConfigValidator.ValidationResult result = validator.validate("pool.size", "0");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("最大边界值应校验通过")
        void maxValueShouldBeValid() {
            ConfigValidator.ValidationResult result = validator.validate("pool.size", "100");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("小于最小值的值应校验失败")
        void valueBelowMinShouldBeInvalid() {
            ConfigValidator.ValidationResult result = validator.validate("pool.size", "-1");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues()).hasSize(1);
            assertThat(result.getIssues().get(0).getMessage()).contains("out of range");
        }

        @Test
        @DisplayName("大于最大值的值应校验失败")
        void valueAboveMaxShouldBeInvalid() {
            ConfigValidator.ValidationResult result = validator.validate("pool.size", "101");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues()).hasSize(1);
        }

        @Test
        @DisplayName("非数值字符串应校验失败")
        void nonNumericValueShouldBeInvalid() {
            ConfigValidator.ValidationResult result = validator.validate("pool.size", "abc");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues().get(0).getMessage()).contains("not a valid number");
        }

        @Test
        @DisplayName("null 值应校验通过（由 required() 处理必填）")
        void nullValueShouldBeValid() {
            ConfigValidator.ValidationResult result = validator.validate("pool.size", null);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("空字符串应校验通过（由 required() 处理必填）")
        void emptyValueShouldBeValid() {
            ConfigValidator.ValidationResult result = validator.validate("pool.size", "");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("min > max 应抛出 IllegalArgumentException")
        void minGreaterThanMaxShouldThrowException() {
            assertThatThrownBy(() -> ConfigValidator.range(100, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("min must not be greater than max");
        }

        @Test
        @DisplayName("toString 应包含范围信息")
        void toStringShouldContainRangeInfo() {
            assertThat(validator.toString()).isEqualTo("ConfigValidator.range(0, 100)");
        }
    }

    @Nested
    @DisplayName("正则模式校验器 pattern()")
    class PatternValidatorTests {

        private final ConfigValidator validator = ConfigValidator.pattern("^[a-z][a-z0-9-]+$");

        @Test
        @DisplayName("匹配正则的值应校验通过")
        void valueMatchingPatternShouldBeValid() {
            ConfigValidator.ValidationResult result = validator.validate("app.name", "my-app");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("不匹配正则的值应校验失败")
        void valueNotMatchingPatternShouldBeInvalid() {
            ConfigValidator.ValidationResult result = validator.validate("app.name", "MyApp");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues().get(0).getMessage()).contains("does not match pattern");
        }

        @Test
        @DisplayName("null 值应校验通过（由 required() 处理必填）")
        void nullValueShouldBeValid() {
            ConfigValidator.ValidationResult result = validator.validate("app.name", null);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("toString 应包含正则信息")
        void toStringShouldContainPatternInfo() {
            assertThat(validator.toString()).contains("^[a-z][a-z0-9-]+$");
        }
    }

    @Nested
    @DisplayName("ValidationResult")
    class ValidationResultTests {

        @Test
        @DisplayName("valid() 应返回有效结果")
        void validFactoryShouldReturnValidResult() {
            ConfigValidator.ValidationResult result = ConfigValidator.ValidationResult.valid();

            assertThat(result.isValid()).isTrue();
            assertThat(result.getIssues()).isEmpty();
        }

        @Test
        @DisplayName("invalid() 应返回无效结果并包含问题列表")
        void invalidFactoryShouldReturnInvalidResult() {
            ConfigValidator.ValidationIssue issue = new ConfigValidator.ValidationIssue(
                    ConfigValidator.ValidationIssue.Severity.ERROR, "key", "message");
            ConfigValidator.ValidationResult result = ConfigValidator.ValidationResult.invalid(
                    List.of(issue));

            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues()).hasSize(1);
        }

        @Test
        @DisplayName("invalid() 单个问题应返回无效结果")
        void invalidSingleIssueFactoryShouldReturnInvalidResult() {
            ConfigValidator.ValidationIssue issue = new ConfigValidator.ValidationIssue(
                    ConfigValidator.ValidationIssue.Severity.WARN, "key", "warning");
            ConfigValidator.ValidationResult result = ConfigValidator.ValidationResult.invalid(issue);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getIssues()).hasSize(1);
            assertThat(result.getIssues().get(0).getSeverity())
                    .isEqualTo(ConfigValidator.ValidationIssue.Severity.WARN);
        }
    }

    @Nested
    @DisplayName("ValidationIssue")
    class ValidationIssueTests {

        @Test
        @DisplayName("应正确创建校验问题")
        void shouldCreateValidationIssue() {
            ConfigValidator.ValidationIssue issue = new ConfigValidator.ValidationIssue(
                    ConfigValidator.ValidationIssue.Severity.ERROR, "db.url", "Required value missing");

            assertThat(issue.getSeverity()).isEqualTo(ConfigValidator.ValidationIssue.Severity.ERROR);
            assertThat(issue.getKey()).isEqualTo("db.url");
            assertThat(issue.getMessage()).isEqualTo("Required value missing");
        }

        @Test
        @DisplayName("null severity 应抛出 NullPointerException")
        void nullSeverityShouldThrowException() {
            assertThatThrownBy(() -> new ConfigValidator.ValidationIssue(
                    null, "key", "message"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("toString 应包含所有字段信息")
        void toStringShouldContainAllFields() {
            ConfigValidator.ValidationIssue issue = new ConfigValidator.ValidationIssue(
                    ConfigValidator.ValidationIssue.Severity.ERROR, "db.url", "missing");

            assertThat(issue.toString()).contains("ERROR");
            assertThat(issue.toString()).contains("db.url");
            assertThat(issue.toString()).contains("missing");
        }
    }
}
