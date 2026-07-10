package com.microservice.framework.common.error;

/**
 * Framework-level runtime exception that carries a structured {@link FrameworkErrorCode}.
 * <p>
 * The error code is always included in the message so that logging and monitoring
 * can extract it without inspecting the exception type.
 *
 * @author Andy Yang
 */
public class FrameworkException extends RuntimeException {

    private final FrameworkErrorCode errorCode;

    /**
     * Creates an exception with the error code as the message.
     *
     * @param errorCode the structured error code
     */
    public FrameworkException(FrameworkErrorCode errorCode) {
        super(errorCode.code());
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception with a custom message prefixed by the error code.
     *
     * @param errorCode the structured error code
     * @param message   additional detail beyond the code
     */
    public FrameworkException(FrameworkErrorCode errorCode, String message) {
        super(formatMessage(errorCode, message));
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception with a custom message, error code prefix, and a cause.
     *
     * @param errorCode the structured error code
     * @param message   additional detail beyond the code
     * @param cause     the underlying cause
     */
    public FrameworkException(FrameworkErrorCode errorCode, String message, Throwable cause) {
        super(formatMessage(errorCode, message), cause);
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception with the error code as the message and a cause.
     *
     * @param errorCode the structured error code
     * @param cause     the underlying cause
     */
    public FrameworkException(FrameworkErrorCode errorCode, Throwable cause) {
        super(errorCode.code(), cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the structured error code carried by this exception.
     *
     * @return the error code
     */
    public FrameworkErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the message with the error code prefix, e.g. {@code "[COMMON-TIME-001] specific message"}.
     *
     * @return formatted message
     */
    @Override
    public String getMessage() {
        return super.getMessage();
    }

    private static String formatMessage(FrameworkErrorCode errorCode, String message) {
        return "[" + errorCode.code() + "] " + message;
    }
}
