package com.microservice.framework.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FrameworkErrorCode 单元测试
 * <p>
 * 验证错误码的创建、格式化、参数校验、相等性以及预定义常量。
 *
 * @author Andy Yang
 */
class FrameworkErrorCodeTest {

    @Nested
    @DisplayName("创建与格式化")
    class CreationAndFormatting {

        @Test
        @DisplayName("使用 of() 创建有效错误码")
        void shouldCreateValidErrorCode() {
            FrameworkErrorCode code = FrameworkErrorCode.of("COMMON", "TIME", 1);
            assertThat(code).isNotNull();
            assertThat(code.getModule()).isEqualTo("COMMON");
            assertThat(code.getCategory()).isEqualTo("TIME");
            assertThat(code.getNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("code() 格式应为 MODULE-CATEGORY-NUMBER")
        void codeFormatShouldBeModuleCategoryNumber() {
            FrameworkErrorCode code = FrameworkErrorCode.of("COMMON", "TIME", 1);
            assertThat(code.code()).isEqualTo("COMMON-TIME-001");
        }

        @Test
        @DisplayName("编号应零填充到 3 位")
        void numberShouldBeZeroPaddedToThreeDigits() {
            FrameworkErrorCode code = FrameworkErrorCode.of("COMMON", "TIME", 1);
            assertThat(code.code()).isEqualTo("COMMON-TIME-001");
            assertThat(code.code()).doesNotContain("COMMON-TIME-1");
        }

        @Test
        @DisplayName("大编号应正常格式化（超出 3 位）")
        void largeNumberShouldFormatCorrectly() {
            FrameworkErrorCode code = FrameworkErrorCode.of("COMMON", "TIME", 100);
            assertThat(code.code()).isEqualTo("COMMON-TIME-100");
        }

        @Test
        @DisplayName("toString 应返回 code()")
        void toStringShouldReturnCode() {
            FrameworkErrorCode code = FrameworkErrorCode.of("COMMON", "TIME", 1);
            assertThat(code.toString()).isEqualTo(code.code());
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ParameterValidation {

        @Test
        @DisplayName("小写模块名应抛出 IllegalArgumentException")
        void lowercaseModuleShouldThrow() {
            assertThatThrownBy(() -> FrameworkErrorCode.of("common", "TIME", 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("module must match");
        }

        @Test
        @DisplayName("空模块名应抛出 IllegalArgumentException")
        void emptyModuleShouldThrow() {
            assertThatThrownBy(() -> FrameworkErrorCode.of("", "TIME", 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("module must match");
        }

        @Test
        @DisplayName("null 模块名应抛出 IllegalArgumentException")
        void nullModuleShouldThrow() {
            assertThatThrownBy(() -> FrameworkErrorCode.of(null, "TIME", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("小写类别应抛出 IllegalArgumentException")
        void lowercaseCategoryShouldThrow() {
            assertThatThrownBy(() -> FrameworkErrorCode.of("COMMON", "time", 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("category must match");
        }

        @Test
        @DisplayName("空类别应抛出 IllegalArgumentException")
        void emptyCategoryShouldThrow() {
            assertThatThrownBy(() -> FrameworkErrorCode.of("COMMON", "", 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("category must match");
        }

        @Test
        @DisplayName("负数编号应抛出 IllegalArgumentException")
        void negativeNumberShouldThrow() {
            assertThatThrownBy(() -> FrameworkErrorCode.of("COMMON", "TIME", -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("number must be positive");
        }

        @Test
        @DisplayName("零编号应抛出 IllegalArgumentException")
        void zeroNumberShouldThrow() {
            assertThatThrownBy(() -> FrameworkErrorCode.of("COMMON", "TIME", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("number must be positive");
        }
    }

    @Nested
    @DisplayName("相等性与哈希")
    class EqualityAndHash {

        @Test
        @DisplayName("相同参数的错误码应相等")
        void sameParametersShouldBeEqual() {
            FrameworkErrorCode code1 = FrameworkErrorCode.of("COMMON", "TIME", 1);
            FrameworkErrorCode code2 = FrameworkErrorCode.of("COMMON", "TIME", 1);
            assertThat(code1).isEqualTo(code2);
            assertThat(code1.hashCode()).isEqualTo(code2.hashCode());
        }

        @Test
        @DisplayName("不同参数的错误码应不相等")
        void differentParametersShouldNotBeEqual() {
            FrameworkErrorCode code1 = FrameworkErrorCode.of("COMMON", "TIME", 1);
            FrameworkErrorCode code2 = FrameworkErrorCode.of("COMMON", "ID", 1);
            assertThat(code1).isNotEqualTo(code2);
        }

        @Test
        @DisplayName("相同对象应自等")
        void sameObjectShouldBeSelfEqual() {
            FrameworkErrorCode code = FrameworkErrorCode.of("COMMON", "TIME", 1);
            assertThat(code).isEqualTo(code);
        }

        @Test
        @DisplayName("与 null 比较应不相等")
        void comparedWithNullShouldNotBeEqual() {
            FrameworkErrorCode code = FrameworkErrorCode.of("COMMON", "TIME", 1);
            assertThat(code).isNotEqualTo(null);
        }

        @Test
        @DisplayName("与不同类型比较应不相等")
        void comparedWithDifferentTypeShouldNotBeEqual() {
            FrameworkErrorCode code = FrameworkErrorCode.of("COMMON", "TIME", 1);
            assertThat(code).isNotEqualTo("COMMON-TIME-001");
        }
    }

    @Nested
    @DisplayName("预定义常量")
    class PredefinedConstants {

        @Test
        @DisplayName("COMMON_TIME_INVALID_ZONE 格式正确")
        void commonTimeInvalidZoneShouldHaveCorrectFormat() {
            assertThat(FrameworkErrorCode.COMMON_TIME_INVALID_ZONE.code())
                    .isEqualTo("COMMON-TIME-001");
        }

        @Test
        @DisplayName("COMMON_ID_WORKER_INVALID 格式正确")
        void commonIdWorkerInvalidShouldHaveCorrectFormat() {
            assertThat(FrameworkErrorCode.COMMON_ID_WORKER_INVALID.code())
                    .isEqualTo("COMMON-ID-001");
        }

        @Test
        @DisplayName("COMMON_ID_CLOCK_BACKWARD 格式正确")
        void commonIdClockBackwardShouldHaveCorrectFormat() {
            assertThat(FrameworkErrorCode.COMMON_ID_CLOCK_BACKWARD.code())
                    .isEqualTo("COMMON-ID-002");
        }

        @Test
        @DisplayName("COMMON_ID_SEQUENCE_OVERFLOW 格式正确")
        void commonIdSequenceOverflowShouldHaveCorrectFormat() {
            assertThat(FrameworkErrorCode.COMMON_ID_SEQUENCE_OVERFLOW.code())
                    .isEqualTo("COMMON-ID-003");
        }

        @Test
        @DisplayName("COMMON_ID_CONFIG_INVALID 格式正确")
        void commonIdConfigInvalidShouldHaveCorrectFormat() {
            assertThat(FrameworkErrorCode.COMMON_ID_CONFIG_INVALID.code())
                    .isEqualTo("COMMON-ID-004");
        }
    }
}
