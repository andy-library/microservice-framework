package com.microservice.framework.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable money value object with a fixed scale of 2 decimal places.
 * <p>
 * Currency is an ISO 4217 3-letter code (default: "CNY").
 * All arithmetic operations use {@code RoundingMode.HALF_UP} with scale=2.
 * Only {@code BigDecimal} is accepted for amounts in the API — float/double
 * are intentionally excluded to avoid precision loss.
 *
 * @author Andy Yang
 */
public final class Money {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final String DEFAULT_CURRENCY = "CNY";

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount.setScale(SCALE, ROUNDING_MODE);
        this.currency = validateCurrency(currency);
    }

    /**
     * Factory method using the default currency ("CNY").
     *
     * @param amount the monetary amount
     * @return a new {@code Money}
     */
    public static Money of(BigDecimal amount) {
        return of(amount, DEFAULT_CURRENCY);
    }

    /**
     * Factory method with an explicit currency.
     *
     * @param amount   the monetary amount
     * @param currency ISO 4217 3-letter currency code
     * @return a new {@code Money}
     * @throws IllegalArgumentException if currency is not a 3-letter code
     * @throws NullPointerException     if amount or currency is null
     */
    public static Money of(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "amount must be non-null");
        Objects.requireNonNull(currency, "currency must be non-null");
        return new Money(amount, currency);
    }

    /**
     * Factory method that converts a double value (in yuan) to {@code Money}.
     * <p>
     * This is a convenience for numeric literals. The double is immediately
     * converted to {@code BigDecimal} to avoid ongoing float precision issues.
     *
     * @param yuan the amount in yuan
     * @return a new {@code Money} with default currency
     */
    public static Money ofYuan(double yuan) {
        return of(BigDecimal.valueOf(yuan));
    }

    /**
     * Adds another money value. Both must share the same currency.
     *
     * @param other the money to add
     * @return a new {@code Money} representing the sum
     * @throws IllegalArgumentException if currencies differ
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    /**
     * Subtracts another money value. Both must share the same currency.
     *
     * @param other the money to subtract
     * @return a new {@code Money} representing the difference
     * @throws IllegalArgumentException if currencies differ
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    /**
     * Multiplies by a double factor, rounding with HALF_UP to 2 decimal places.
     *
     * @param factor the multiplication factor
     * @return a new {@code Money} representing the product
     */
    public Money multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    /**
     * Multiplies by a {@code BigDecimal} factor, rounding with HALF_UP to 2 decimal places.
     *
     * @param factor the multiplication factor
     * @return a new {@code Money} representing the product
     */
    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must be non-null");
        return new Money(amount.multiply(factor).setScale(SCALE, ROUNDING_MODE), currency);
    }

    /**
     * Returns whether the amount is zero.
     *
     * @return true if amount equals 0.00
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Returns whether the amount is positive (greater than zero).
     *
     * @return true if amount &gt; 0.00
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns whether the amount is negative (less than zero).
     *
     * @return true if amount &lt; 0.00
     */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Money)) {
            return false;
        }
        Money other = (Money) obj;
        return amount.equals(other.amount) && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return 31 * amount.hashCode() + currency.hashCode();
    }

    @Override
    public String toString() {
        return currency + " " + amount.toPlainString();
    }

    private static String validateCurrency(String currency) {
        if (currency.length() != 3 || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "currency must be a 3-letter ISO 4217 code, but was: " + currency);
        }
        return currency;
    }

    private void validateSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
