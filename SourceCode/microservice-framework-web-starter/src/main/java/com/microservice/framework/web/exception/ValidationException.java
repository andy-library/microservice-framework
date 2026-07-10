package com.microservice.framework.web.exception;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;

import java.util.List;

/**
 * Validation exception for the Web module.
 * <p>
 * Thrown when request parameter validation fails. Carries a list of
 * field-level validation errors for structured error reporting.
 * <p>
 * Error code format: {@code WEB-VALID-NNN}
 *
 * @author Andy Yang
 */
public class ValidationException extends FrameworkException {

    /** Generic validation failure. */
    public static final FrameworkErrorCode WEB_VALID_GENERAL = FrameworkErrorCode.of("WEB", "VALID", 1);

    /** Required parameter missing. */
    public static final FrameworkErrorCode WEB_VALID_MISSING = FrameworkErrorCode.of("WEB", "VALID", 2);

    /** Parameter format invalid. */
    public static final FrameworkErrorCode WEB_VALID_FORMAT = FrameworkErrorCode.of("WEB", "VALID", 3);

    /** Parameter value out of range. */
    public static final FrameworkErrorCode WEB_VALID_RANGE = FrameworkErrorCode.of("WEB", "VALID", 4);

    private final List<FieldError> fieldErrors;

    public ValidationException(FrameworkErrorCode errorCode) {
        super(errorCode);
        this.fieldErrors = List.of();
    }

    public ValidationException(FrameworkErrorCode errorCode, String message) {
        super(errorCode, message);
        this.fieldErrors = List.of();
    }

    public ValidationException(FrameworkErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.fieldErrors = List.of();
    }

    public ValidationException(FrameworkErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
        this.fieldErrors = List.of();
    }

    public ValidationException(FrameworkErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, formatFieldErrors(fieldErrors));
        this.fieldErrors = fieldErrors != null ? List.copyOf(fieldErrors) : List.of();
    }

    public ValidationException(FrameworkErrorCode errorCode, String message, List<FieldError> fieldErrors) {
        super(errorCode, message);
        this.fieldErrors = fieldErrors != null ? List.copyOf(fieldErrors) : List.of();
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    private static String formatFieldErrors(List<FieldError> errors) {
        if (errors == null || errors.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (FieldError error : errors) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(error.field).append(": ").append(error.message);
        }
        return sb.toString();
    }

    /**
     * Field-level validation error detail.
     *
     * @param field  the field name that failed validation
     * @param message the validation error message
     */
    public record FieldError(String field, String message) {}
}
