package com.microservice.framework.common.util;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Parameter validation utility with descriptive exception messages.
 *
 * <p>Each method throws {@link IllegalArgumentException} with a message
 * that identifies the parameter by name, making debugging easier.</p>
 *
 * @author Andy Yang
 */
public final class Validations {

    /**
     * Prevent instantiation — this class holds only static utility methods.
     */
    private Validations() {}

    /**
     * Validates that the given object is not null.
     *
     * @param obj     the object to check
     * @param message the exception message if the object is null
     * @return the object if it is not null
     * @throws IllegalArgumentException if {@code obj} is {@code null}
     */
    public static Object requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    /**
     * Validates that the given long value is strictly positive (greater than 0).
     *
     * @param value the value to check
     * @param name  the parameter name for the exception message
     * @return the value if it is positive
     * @throws IllegalArgumentException if {@code value} is less than or equal to 0
     */
    public static long requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, but was: " + value);
        }
        return value;
    }

    /**
     * Validates that the given long value falls within the specified range (inclusive).
     *
     * @param value the value to check
     * @param min   the minimum allowed value (inclusive)
     * @param max   the maximum allowed value (inclusive)
     * @param name  the parameter name for the exception message
     * @return the value if it is within range
     * @throws IllegalArgumentException if {@code value} is outside [min, max]
     */
    public static long requireRange(long value, long min, long max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " must be between " + min + " and " + max + ", but was: " + value);
        }
        return value;
    }

    /**
     * Validates that the given string is not blank (not null and not whitespace-only).
     *
     * @param str  the string to check
     * @param name the parameter name for the exception message
     * @return the string if it is not blank
     * @throws IllegalArgumentException if {@code str} is null or whitespace-only
     */
    public static String requireNonBlank(String str, String name) {
        if (str == null || str.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return str;
    }

    /**
     * Validates that the given string matches the specified regular expression.
     *
     * @param str   the string to check
     * @param regex the regular expression pattern
     * @param name  the parameter name for the exception message
     * @return the string if it matches the regex
     * @throws IllegalArgumentException if {@code str} does not match {@code regex},
     *                                  or if {@code str} is null
     */
    public static String requireMatches(String str, String regex, String name) {
        if (str == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        if (!Pattern.matches(regex, str)) {
            throw new IllegalArgumentException(
                    name + " must match pattern '" + regex + "', but was: '" + str + "'");
        }
        return str;
    }
}
