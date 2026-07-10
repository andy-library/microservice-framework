package com.microservice.framework.common.util;

/**
 * String utility class with null-safe semantics.
 *
 * <p>All methods handle {@code null} input gracefully. See individual
 * method documentation for specific null-handling behavior.</p>
 *
 * @author Andy Yang
 */
public final class Strings {

    /**
     * Prevent instantiation — this class holds only static utility methods.
     */
    private Strings() {}

    /**
     * Checks whether the given string is blank.
     *
     * <p>A string is considered blank if it is {@code null}, empty,
     * or consists entirely of whitespace characters.</p>
     *
     * @param str the string to check; {@code null} returns {@code true}
     * @return {@code true} if the string is null or whitespace-only
     */
    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        return str.isBlank();
    }

    /**
     * Checks whether the given string is not blank.
     *
     * <p>This is the inverse of {@link #isBlank(String)}.</p>
     *
     * @param str the string to check
     * @return {@code true} if the string contains at least one non-whitespace character
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Returns the default value if the given string is blank.
     *
     * @param str          the string to evaluate; {@code null} is treated as blank
     * @param defaultValue the fallback value returned when {@code str} is blank
     * @return {@code str} if it is not blank, otherwise {@code defaultValue}
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        if (isBlank(str)) {
            return defaultValue;
        }
        return str;
    }

    /**
     * Truncates the string to the given maximum length, appending "..." if it exceeds that length.
     *
     * <p>If {@code str} is {@code null}, {@code null} is returned.
     * If {@code maxLength} is less than 3, the string is truncated without the ellipsis suffix.</p>
     *
     * @param str       the string to truncate; {@code null} returns {@code null}
     * @param maxLength the maximum length of the result (including the "..." suffix)
     * @return the truncated string, or {@code null} if input was {@code null}
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        if (maxLength < 3) {
            return str.substring(0, maxLength);
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Throws {@link IllegalArgumentException} if the given string is blank.
     *
     * @param str     the string to validate
     * @param message the exception message if the string is blank
     * @return the string if it is not blank
     * @throws IllegalArgumentException if {@code str} is null or whitespace-only
     */
    public static String requireNonBlank(String str, String message) {
        if (isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }
}
