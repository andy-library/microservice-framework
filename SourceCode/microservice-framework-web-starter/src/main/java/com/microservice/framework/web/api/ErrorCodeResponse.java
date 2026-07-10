package com.microservice.framework.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Objects;

/**
 * API response variant for structured error responses.
 * <p>
 * Provides additional error metadata beyond the basic code/message pair:
 * an error code string, an optional details map, and an optional stack trace
 * (for development environments).
 * <p>
 * Instances are immutable; use static factory methods to create.
 *
 * @author Andy Yang
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ErrorCodeResponse extends ApiResponse<Object> {

    private final String errorCode;
    private final Map<String, String> details;
    private final String stackTrace;

    private ErrorCodeResponse(int code, String message, Object data,
                              Long timestamp, String requestId,
                              String errorCode, Map<String, String> details, String stackTrace) {
        super(code, message, data, timestamp, requestId);
        this.errorCode = errorCode;
        this.details = details;
        this.stackTrace = stackTrace;
    }

    // ======================================================================
    // Static factory methods
    // ======================================================================

    /**
     * Create an error response from a structured error code string.
     *
     * @param code      numeric error code (typically -1)
     * @param message   human-readable error message
     * @param errorCode structured error code string (e.g., "WEB-VALID-001")
     * @return ErrorCodeResponse with provided fields
     */
    public static ErrorCodeResponse of(int code, String message, String errorCode) {
        return new ErrorCodeResponse(code, message, null,
                System.currentTimeMillis(), null,
                errorCode, null, null);
    }

    /**
     * Create an error response with validation details.
     *
     * @param code      numeric error code
     * @param message   human-readable error message
     * @param errorCode structured error code string
     * @param details   field-level validation errors (field -> error message)
     * @return ErrorCodeResponse with validation details
     */
    public static ErrorCodeResponse of(int code, String message, String errorCode,
                                        Map<String, String> details) {
        return new ErrorCodeResponse(code, message, null,
                System.currentTimeMillis(), null,
                errorCode, details, null);
    }

    /**
     * Create an error response with all metadata.
     *
     * @param code       numeric error code
     * @param message    human-readable error message
     * @param errorCode  structured error code string
     * @param details    field-level validation errors
     * @param stackTrace exception stack trace (for development environments)
     * @return ErrorCodeResponse with all metadata
     */
    public static ErrorCodeResponse of(int code, String message, String errorCode,
                                        Map<String, String> details, String stackTrace) {
        return new ErrorCodeResponse(code, message, null,
                System.currentTimeMillis(), null,
                errorCode, details, stackTrace);
    }

    /**
     * Create a full error response with every field including request ID.
     *
     * @param code       numeric error code
     * @param message    human-readable error message
     * @param errorCode  structured error code string
     * @param details    field-level validation errors
     * @param stackTrace exception stack trace
     * @param timestamp  epoch millis
     * @param requestId  request ID
     * @return ErrorCodeResponse with all fields
     */
    public static ErrorCodeResponse of(int code, String message, String errorCode,
                                        Map<String, String> details, String stackTrace,
                                        Long timestamp, String requestId) {
        return new ErrorCodeResponse(code, message, null,
                timestamp, requestId,
                errorCode, details, stackTrace);
    }

    // ======================================================================
    // Builder-style withXxx methods
    // ======================================================================

    /**
     * Return a copy of this response with the request ID set.
     */
    public ErrorCodeResponse withRequestId(String requestId) {
        return new ErrorCodeResponse(this.getCode(), this.getMessage(), this.getData(),
                this.getTimestamp(), requestId,
                this.errorCode, this.details, this.stackTrace);
    }

    /**
     * Return a copy of this response with the timestamp removed.
     */
    public ErrorCodeResponse withoutTimestamp() {
        return new ErrorCodeResponse(this.getCode(), this.getMessage(), this.getData(),
                null, this.getRequestId(),
                this.errorCode, this.details, this.stackTrace);
    }

    /**
     * Return a copy of this response without the stack trace.
     */
    public ErrorCodeResponse withoutStackTrace() {
        return new ErrorCodeResponse(this.getCode(), this.getMessage(), this.getData(),
                this.getTimestamp(), this.getRequestId(),
                this.errorCode, this.details, null);
    }

    // ======================================================================
    // Getters
    // ======================================================================

    public String getErrorCode() { return errorCode; }
    public Map<String, String> getDetails() { return details; }
    public String getStackTrace() { return stackTrace; }

    // ======================================================================
    // equals / hashCode / toString
    // ======================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ErrorCodeResponse)) return false;
        ErrorCodeResponse other = (ErrorCodeResponse) obj;
        return super.equals(obj)
                && Objects.equals(errorCode, other.errorCode)
                && Objects.equals(details, other.details)
                && Objects.equals(stackTrace, other.stackTrace);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(errorCode);
        result = 31 * result + Objects.hashCode(details);
        result = 31 * result + Objects.hashCode(stackTrace);
        return result;
    }

    @Override
    public String toString() {
        return "ErrorCodeResponse{code=" + getCode()
                + ", message='" + getMessage() + '\''
                + ", errorCode='" + errorCode + '\''
                + ", details=" + details
                + ", requestId='" + getRequestId() + '\''
                + '}';
    }
}
