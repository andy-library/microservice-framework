package com.microservice.framework.common.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Money 单元测试
 * <p>
 * 验证金额值对象的工厂方法、算术运算、舍入模式、状态判断、
 * 货币校验、toString 格式以及相等性。
 *
 * @author Andy Yang
 */
class MoneyTest {

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethods {

        @Test
        @DisplayName("of(BigDecimal) 应使用默认货币 CNY")
        void ofWithBigDecimalShouldUseDefaultCurrency() {
            Money money = Money.of(new BigDecimal("100.00"));
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100.00"));
            assertThat(money.getCurrency()).isEqualTo("CNY");
        }

        @Test
        @DisplayName("of(BigDecimal, String) 应使用指定货币")
        void ofWithBigDecimalAndCurrencyShouldUseSpecifiedCurrency() {
            Money money = Money.of(new BigDecimal("50.00"), "USD");
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("50.00"));
            assertThat(money.getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("ofYuan(double) 应正确转换")
        void ofYuanShouldConvertDouble() {
            Money money = Money.ofYuan(99.99);
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("99.99"));
            assertThat(money.getCurrency()).isEqualTo("CNY");
        }

        @Test
        @DisplayName("of() null amount 应抛出 NullPointerException")
        void ofWithNullAmountShouldThrow() {
            assertThatThrownBy(() -> Money.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("amount must be non-null");
        }

        @Test
        @DisplayName("of() null currency 应抛出 NullPointerException")
        void ofWithNullCurrencyShouldThrow() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("100"), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("currency must be non-null");
        }
    }

    @Nested
    @DisplayName("舍入")
    class Rounding {

        @Test
        @DisplayName("金额应使用 HALF_UP 舍入到 2 位小数")
        void amountShouldRoundHalfUpToTwoDecimalPlaces() {
            Money money = Money.of(new BigDecimal("100.125"));
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100.13"));
        }

        @Test
        @DisplayName("超过 2 位的金额应自动舍入")
        void amountWithMoreThanTwoDecimalsShouldAutoRound() {
            Money money = Money.of(new BigDecimal("99.999"));
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("算术运算")
    class ArithmeticOperations {

        @Test
        @DisplayName("add() 应正确加法")
        void addShouldSumCorrectly() {
            Money a = Money.of(new BigDecimal("50.00"));
            Money b = Money.of(new BigDecimal("30.00"));
            assertThat(a.add(b).getAmount()).isEqualTo(new BigDecimal("80.00"));
        }

        @Test
        @DisplayName("subtract() 应正确减法")
        void subtractShouldDifferenceCorrectly() {
            Money a = Money.of(new BigDecimal("50.00"));
            Money b = Money.of(new BigDecimal("30.00"));
            assertThat(a.subtract(b).getAmount()).isEqualTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("multiply(double) 应正确乘法")
        void multiplyDoubleShouldMultiplyCorrectly() {
            Money money = Money.of(new BigDecimal("100.00"));
            assertThat(money.multiply(1.5).getAmount()).isEqualTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("multiply(BigDecimal) 应正确乘法并舍入")
        void multiplyBigDecimalShouldMultiplyAndRound() {
            Money money = Money.of(new BigDecimal("100.00"));
            assertThat(money.multiply(new BigDecimal("0.333")).getAmount())
                    .isEqualTo(new BigDecimal("33.30"));
        }

        @Test
        @DisplayName("不同货币的 add() 应抛出 IllegalArgumentException")
        void addWithDifferentCurrencyShouldThrow() {
            Money cny = Money.of(new BigDecimal("100.00"), "CNY");
            Money usd = Money.of(new BigDecimal("100.00"), "USD");
            assertThatThrownBy(() -> cny.add(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currency mismatch");
        }

        @Test
        @DisplayName("不同货币的 subtract() 应抛出 IllegalArgumentException")
        void subtractWithDifferentCurrencyShouldThrow() {
            Money cny = Money.of(new BigDecimal("100.00"), "CNY");
            Money usd = Money.of(new BigDecimal("100.00"), "USD");
            assertThatThrownBy(() -> cny.subtract(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currency mismatch");
        }

        @Test
        @DisplayName("multiply(null) 应抛出 NullPointerException")
        void multiplyWithNullShouldThrow() {
            Money money = Money.of(new BigDecimal("100.00"));
            assertThatThrownBy(() -> money.multiply((BigDecimal) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("factor must be non-null");
        }
    }

    @Nested
    @DisplayName("状态判断")
    class StateChecks {

        @Test
        @DisplayName("isZero() 对零金额应返回 true")
        void isZeroShouldReturnTrueForZeroAmount() {
            assertThat(Money.of(new BigDecimal("0.00")).isZero()).isTrue();
        }

        @Test
        @DisplayName("isZero() 对正金额应返回 false")
        void isZeroShouldReturnFalseForPositiveAmount() {
            assertThat(Money.of(new BigDecimal("100.00")).isZero()).isFalse();
        }

        @Test
        @DisplayName("isPositive() 对正金额应返回 true")
        void isPositiveShouldReturnTrueForPositiveAmount() {
            assertThat(Money.of(new BigDecimal("100.00")).isPositive()).isTrue();
        }

        @Test
        @DisplayName("isPositive() 对零金额应返回 false")
        void isPositiveShouldReturnFalseForZeroAmount() {
            assertThat(Money.of(new BigDecimal("0.00")).isPositive()).isFalse();
        }

        @Test
        @DisplayName("isPositive() 对负金额应返回 false")
        void isPositiveShouldReturnFalseForNegativeAmount() {
            assertThat(Money.of(new BigDecimal("-10.00")).isPositive()).isFalse();
        }

        @Test
        @DisplayName("isNegative() 对负金额应返回 true")
        void isNegativeShouldReturnTrueForNegativeAmount() {
            assertThat(Money.of(new BigDecimal("-10.00")).isNegative()).isTrue();
        }

        @Test
        @DisplayName("isNegative() 对零金额应返回 false")
        void isNegativeShouldReturnFalseForZeroAmount() {
            assertThat(Money.of(new BigDecimal("0.00")).isNegative()).isFalse();
        }

        @Test
        @DisplayName("isNegative() 对正金额应返回 false")
        void isNegativeShouldReturnFalseForPositiveAmount() {
            assertThat(Money.of(new BigDecimal("100.00")).isNegative()).isFalse();
        }
    }

    @Nested
    @DisplayName("货币校验")
    class CurrencyValidation {

        @Test
        @DisplayName("3 字母大写货币代码应通过校验")
        void threeLetterUpperCaseCurrencyShouldPass() {
            Money money = Money.of(new BigDecimal("100.00"), "EUR");
            assertThat(money.getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("非 3 字母代码应抛出 IllegalArgumentException")
        void nonThreeLetterCurrencyShouldThrow() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("100.00"), "US"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currency must be a 3-letter ISO 4217 code");
        }

        @Test
        @DisplayName("包含小写的货币代码应抛出 IllegalArgumentException")
        void currencyWithLowercaseShouldThrow() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("100.00"), "usd"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currency must be a 3-letter ISO 4217 code");
        }

        @Test
        @DisplayName("4 字母货币代码应抛出 IllegalArgumentException")
        void fourLetterCurrencyShouldThrow() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("100.00"), "USDD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currency must be a 3-letter ISO 4217 code");
        }
    }

    @Nested
    @DisplayName("toString 与相等性")
    class ToStringAndEquality {

        @Test
        @DisplayName("toString 格式应为 CURRENCY AMOUNT")
        void toStringShouldFormatAsCurrencyAmount() {
            Money money = Money.of(new BigDecimal("100.00"));
            assertThat(money.toString()).isEqualTo("CNY 100.00");
        }

        @Test
        @DisplayName("toString 应使用 toPlainString 避免科学记数法")
        void toStringShouldUsePlainFormat() {
            Money money = Money.of(new BigDecimal("0.01"));
            assertThat(money.toString()).doesNotContain("E");
        }

        @Test
        @DisplayName("相同金额和货币应相等")
        void sameAmountAndCurrencyShouldBeEqual() {
            Money money1 = Money.of(new BigDecimal("100.00"));
            Money money2 = Money.of(new BigDecimal("100.00"));
            assertThat(money1).isEqualTo(money2);
            assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
        }

        @Test
        @DisplayName("不同金额应不相等")
        void differentAmountShouldNotBeEqual() {
            Money money1 = Money.of(new BigDecimal("100.00"));
            Money money2 = Money.of(new BigDecimal("200.00"));
            assertThat(money1).isNotEqualTo(money2);
        }

        @Test
        @DisplayName("不同货币应不相等")
        void differentCurrencyShouldNotBeEqual() {
            Money money1 = Money.of(new BigDecimal("100.00"), "CNY");
            Money money2 = Money.of(new BigDecimal("100.00"), "USD");
            assertThat(money1).isNotEqualTo(money2);
        }
    }
}
