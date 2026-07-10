package com.microservice.framework.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Strings 单元测试
 * <p>
 * 验证字符串工具类的 isBlank/isNotBlank、defaultIfBlank、truncate 以及 requireNonBlank。
 *
 * @author Andy Yang
 */
class StringsTest {

    @Nested
    @DisplayName("isBlank / isNotBlank")
    class BlankChecks {

        @Test
        @DisplayName("isBlank(null) 应返回 true")
        void isBlankWithNullShouldReturnTrue() {
            assertThat(Strings.isBlank(null)).isTrue();
        }

        @Test
        @DisplayName("isBlank(空字符串) 应返回 true")
        void isBlankWithEmptyStringShouldReturnTrue() {
            assertThat(Strings.isBlank("")).isTrue();
        }

        @Test
        @DisplayName("isBlank(纯空白) 应返回 true")
        void isBlankWithWhitespaceShouldReturnTrue() {
            assertThat(Strings.isBlank("   ")).isTrue();
            assertThat(Strings.isBlank("\t\n\r")).isTrue();
        }

        @Test
        @DisplayName("isBlank(普通字符串) 应返回 false")
        void isBlankWithNormalStringShouldReturnFalse() {
            assertThat(Strings.isBlank("hello")).isFalse();
            assertThat(Strings.isBlank("  hello  ")).isFalse();
        }

        @Test
        @DisplayName("isNotBlank 是 isBlank 的逆运算")
        void isNotBlankShouldBeInverseOfIsBlank() {
            assertThat(Strings.isNotBlank(null)).isFalse();
            assertThat(Strings.isNotBlank("")).isFalse();
            assertThat(Strings.isNotBlank("   ")).isFalse();
            assertThat(Strings.isNotBlank("hello")).isTrue();
        }
    }

    @Nested
    @DisplayName("defaultIfBlank")
    class DefaultIfBlank {

        @Test
        @DisplayName("blank 字符串应返回默认值")
        void blankStringShouldReturnDefault() {
            assertThat(Strings.defaultIfBlank(null, "default")).isEqualTo("default");
            assertThat(Strings.defaultIfBlank("", "default")).isEqualTo("default");
            assertThat(Strings.defaultIfBlank("   ", "default")).isEqualTo("default");
        }

        @Test
        @DisplayName("非 blank 字符串应返回原值")
        void nonBlankStringShouldReturnOriginal() {
            assertThat(Strings.defaultIfBlank("hello", "default")).isEqualTo("hello");
            assertThat(Strings.defaultIfBlank("  hello  ", "default")).isEqualTo("  hello  ");
        }
    }

    @Nested
    @DisplayName("truncate")
    class Truncate {

        @Test
        @DisplayName("null 输入应返回 null")
        void truncateNullShouldReturnNull() {
            assertThat(Strings.truncate(null, 10)).isNull();
        }

        @Test
        @DisplayName("短字符串不应截断")
        void shortStringShouldNotBeTruncated() {
            assertThat(Strings.truncate("hello", 10)).isEqualTo("hello");
        }

        @Test
        @DisplayName("长字符串应截断并添加 ...")
        void longStringShouldBeTruncatedWithEllipsis() {
            assertThat(Strings.truncate("hello world", 8)).isEqualTo("hello...");
        }

        @Test
        @DisplayName("maxLength < 3 应截断不添加 ...")
        void maxLengthLessThanThreeShouldTruncateWithoutEllipsis() {
            assertThat(Strings.truncate("hello", 2)).isEqualTo("he");
        }

        @Test
        @DisplayName("恰好 maxLength 的字符串不应截断")
        void exactlyMaxLengthShouldNotBeTruncated() {
            assertThat(Strings.truncate("hello", 5)).isEqualTo("hello");
        }
    }

    @Nested
    @DisplayName("requireNonBlank")
    class RequireNonBlank {

        @Test
        @DisplayName("非 blank 字符串应返回原值")
        void nonBlankStringShouldReturnOriginal() {
            assertThat(Strings.requireNonBlank("hello", "must not be blank")).isEqualTo("hello");
        }

        @Test
        @DisplayName("null 字符串应抛出 IllegalArgumentException")
        void nullStringShouldThrow() {
            assertThatThrownBy(() -> Strings.requireNonBlank(null, "must not be blank"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("must not be blank");
        }

        @Test
        @DisplayName("空字符串应抛出 IllegalArgumentException")
        void emptyStringShouldThrow() {
            assertThatThrownBy(() -> Strings.requireNonBlank("", "must not be blank"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("must not be blank");
        }

        @Test
        @DisplayName("纯空白字符串应抛出 IllegalArgumentException")
        void whitespaceStringShouldThrow() {
            assertThatThrownBy(() -> Strings.requireNonBlank("   ", "must not be blank"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("must not be blank");
        }
    }
}
