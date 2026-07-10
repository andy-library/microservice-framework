package com.microservice.framework.common.error;

/**
 * Immutable error code model representing a structured error identifier.
 * <p>
 * Format: {@code <MODULE>-<CATEGORY>-<NUMBER>} (e.g., {@code COMMON-TIME-001}).
 * Module and category must match {@code [A-Z][A-Z0-9_]{0,19}}; number must be positive.
 *
 * @author Andy Yang
 */
public final class FrameworkErrorCode {

    private static final String SEGMENT_PATTERN = "[A-Z][A-Z0-9_]{0,19}";

    /** Invalid time zone. */
    public static final FrameworkErrorCode COMMON_TIME_INVALID_ZONE = of("COMMON", "TIME", 1);

    /** Snowflake ID worker number invalid. */
    public static final FrameworkErrorCode COMMON_ID_WORKER_INVALID = of("COMMON", "ID", 1);

    /** System clock moved backward, refusing to generate ID. */
    public static final FrameworkErrorCode COMMON_ID_CLOCK_BACKWARD = of("COMMON", "ID", 2);

    /** Sequence overflow within a single millisecond. */
    public static final FrameworkErrorCode COMMON_ID_SEQUENCE_OVERFLOW = of("COMMON", "ID", 3);

    /** ID configuration invalid (e.g., negative tolerance, invalid batch count). */
    public static final FrameworkErrorCode COMMON_ID_CONFIG_INVALID = of("COMMON", "ID", 4);

    private final String module;
    private final String category;
    private final int number;

    private FrameworkErrorCode(String module, String category, int number) {
        if (module == null || !module.matches(SEGMENT_PATTERN)) {
            throw new IllegalArgumentException(
                    "module must match " + SEGMENT_PATTERN + ", but was: " + module);
        }
        if (category == null || !category.matches(SEGMENT_PATTERN)) {
            throw new IllegalArgumentException(
                    "category must match " + SEGMENT_PATTERN + ", but was: " + category);
        }
        if (number <= 0) {
            throw new IllegalArgumentException("number must be positive, but was: " + number);
        }
        this.module = module;
        this.category = category;
        this.number = number;
    }

    /**
     * Static factory method to create a {@code FrameworkErrorCode}.
     *
     * @param module   uppercase module segment
     * @param category uppercase category segment
     * @param number   positive error number
     * @return a new {@code FrameworkErrorCode}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static FrameworkErrorCode of(String module, String category, int number) {
        return new FrameworkErrorCode(module, category, number);
    }

    /**
     * Returns the formatted error code string, e.g. {@code "COMMON-TIME-001"}.
     * The number is zero-padded to 3 digits.
     *
     * @return formatted error code
     */
    public String code() {
        return module + "-" + category + "-" + String.format("%03d", number);
    }

    public String getModule() {
        return module;
    }

    public String getCategory() {
        return category;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return code();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FrameworkErrorCode)) {
            return false;
        }
        FrameworkErrorCode other = (FrameworkErrorCode) obj;
        return module.equals(other.module)
                && category.equals(other.category)
                && number == other.number;
    }

    @Override
    public int hashCode() {
        int result = module.hashCode();
        result = 31 * result + category.hashCode();
        result = 31 * result + number;
        return result;
    }
}
