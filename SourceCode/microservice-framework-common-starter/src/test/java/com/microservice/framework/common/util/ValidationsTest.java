package com.microservice.framework.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validations 单元测试
 * <p>
 * 验证参数校验工具的 requireNonNull、requirePositive、requireRange、
 * requireNonBlank 和 requireMatches。
 *
 * @author Andy Yang
 */
class ValidationsTest {

    @Nested
    @DisplayName("requireNonNull")
    class RequireNonNull {

        @Test
        @DisplayName("非 null 对象应返回原值")
        void nonNullObjectShouldReturnOriginal() {
            Object obj = "value";
            assertThat(Validations.requireNonNull(obj, "must not be null")).isEqualTo("value");
        }

        @Test
        @DisplayName("null 对象应抛出 IllegalArgumentException")
        void nullObjectShouldThrow() {
            assertThatThrownBy(() -> Validations.requireNonNull(null, "must not be null"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("must not be null");
        }
    }

    @Nested
    @DisplayName("requirePositive")
    class RequirePositive {

        @Test
        @DisplayName("正数应返回原值")
        void positiveValueShouldReturnOriginal() {
            assertThat(Validations.requirePositive(10, "value")).isEqualTo(10);
        }

        @Test
        @DisplayName("零应抛出 IllegalArgumentException")
        void zeroShouldThrow() {
            assertThatThrownBy(() -> Validations.requirePositive(0, "value"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value must be positive, but was: 0");
        }

        @Test
        @DisplayName("负数应抛出 IllegalArgumentException")
        void negativeShouldThrow() {
            assertThatThrownBy(() -> Validations.requirePositive(-5, "value"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value must be positive, but was: -5");
        }
    }

    @Nested
    @DisplayName("requireRange")
    class RequireRange {

        @Test
        @DisplayName("范围内的值应返回原值")
        void valueInRangeShouldReturnOriginal() {
            assertThat(Validations.requireRange(5, 1, 10, "value")).isEqualTo(5);
        }

        @Test
        @DisplayName("边界最小值应返回原值")
        void valueAtMinShouldReturnOriginal() {
            assertThat(Validations.requireRange(1, 1, 10, "value")).isEqualTo(1);
        }

        @Test
        @DisplayName("边界最大值应返回原值")
        void valueAtMaxShouldReturnOriginal() {
            assertThat(Validations.requireRange(10, 1, 10, "value")).isEqualTo(10);
        }

        @Test
        @DisplayName("小于最小值应抛出 IllegalArgumentException")
        void valueBelowMinShouldThrow() {
            assertThatThrownBy(() -> Validations.requireRange(0, 1, 10, "value"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value must be between 1 and 10, but was: 0");
        }

        @Test
        @DisplayName("大于最大值应抛出 IllegalArgumentException")
        void valueAboveMaxShouldThrow() {
            assertThatThrownBy(() -> Validations.requireRange(11, 1, 10, "value"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value must be between 1 and 10, but was: 11");
        }
    }

    @Nested
    @DisplayName("requireNonBlank")
    class RequireNonBlank {

        @Test
        @DisplayName("非 blank 字符串应返回原值")
        void nonBlankStringShouldReturnOriginal() {
            assertThat(Validations.requireNonBlank("hello", "field")).isEqualTo("hello");
        }

        @Test
        @DisplayName("null 字符串应抛出 IllegalArgumentException")
        void nullStringShouldThrow() {
            assertThatThrownBy(() -> Validations.requireNonBlank(null, "field"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("field must not be blank");
        }

        @Test
        @DisplayName("空字符串应抛出 IllegalArgumentException")
        void emptyStringShouldThrow() {
            assertThatThrownBy(() -> Validations.requireNonBlank("", "field"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("field must not be blank");
        }

        @Test
        @DisplayName("纯空白字符串应抛出 IllegalArgumentException")
        void whitespaceStringShouldThrow() {
            assertThatThrownBy(() -> Validations.requireNonBlank("   ", "field"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("field must not be blank");
        }
    }

    @Nested
    @DisplayName("requireMatches")
    class RequireMatches {

        @Test
        @DisplayName("匹配正则的字符串应返回原值")
        void matchingStringShouldReturnOriginal() {
            assertThat(Validations.requireMatches("ABC", "[A-Z]{3}", "code")).isEqualTo("ABC");
        }

        @Test
        @DisplayName("不匹配正则应抛出 IllegalArgumentException")
        void nonMatchingStringShouldThrow() {
            assertThatThrownBy(() -> Validations.requireMatches("abc", "[A-Z]{3}", "code"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code must match pattern '[A-Z]{3}', but was: 'abc'");
        }

        @Test
        @DisplayName("null 字符串应抛出 IllegalArgumentException")
        void nullStringShouldThrow() {
            assertThatThrownBy(() -> Validations.requireMatches(null, "[A-Z]{3}", "code"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code must not be null");
        }
    }
}
