package com.microservice.framework.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Objects;

/**
 * Generic API response wrapper.
 * <p>
 * Carries a status code, message, optional data payload, timestamp,
 * and optional request ID. Designed for consistent HTTP response formatting
 * across all microservice endpoints.
 * <p>
 * Instances are immutable; use static factory methods to create:
 * <ul>
 *   <li>{@link #success()} — empty success response</li>
 *   <li>{@link #success(Object)} — success with data</li>
 *   <li>{@link #error(int, String)} — error with code and message</li>
 *   <li>{@link #error(int, String, Object)} — error with code, message and data</li>
 * </ul>
 *
 * @param <T> the type of the data payload
 * @author Andy Yang
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final Long timestamp;
    private final String requestId;

    protected ApiResponse(int code, String message, T data, Long timestamp, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
        this.requestId = requestId;
    }

    // ======================================================================
    // Static factory methods
    // ======================================================================

    /**
     * Create a success response with no data.
     *
     * @return ApiResponse with default success code and message
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "success", null, Instant.now().toEpochMilli(), null);
    }

    /**
     * Create a success response with data payload.
     *
     * @param data the response data
     * @return ApiResponse with default success code, message and provided data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, Instant.now().toEpochMilli(), null);
    }

    /**
     * Create a success response with custom message and data.
     *
     * @param message custom success message
     * @param data    the response data
     * @return ApiResponse with default success code, custom message and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data, Instant.now().toEpochMilli(), null);
    }

    /**
     * Create an error response with code and message.
     *
     * @param code    error code (typically negative)
     * @param message error message
     * @return ApiResponse with provided error code, message, no data
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now().toEpochMilli(), null);
    }

    /**
     * Create an error response with code, message and data.
     *
     * @param code    error code (typically negative)
     * @param message error message
     * @param data    additional error data
     * @return ApiResponse with provided error code, message and data
     */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data, Instant.now().toEpochMilli(), null);
    }

    /**
     * Create a response with all fields explicitly set.
     * <p>
     * Used by auto-configuration to build responses with custom codes
     * and request ID propagation.
     *
     * @param code      response code
     * @param message   response message
     * @param data      response data (may be null)
     * @param timestamp epoch millis (may be null if disabled)
     * @param requestId request ID (may be null if disabled)
     * @return ApiResponse with all fields set
     */
    public static <T> ApiResponse<T> of(int code, String message, T data, Long timestamp, String requestId) {
        return new ApiResponse<>(code, message, data, timestamp, requestId);
    }

    // ======================================================================
    // Builder-style withXxx methods for request ID and timestamp injection
    // ======================================================================

    /**
     * Return a copy of this response with the request ID set.
     *
     * @param requestId the request ID to inject
     * @return new ApiResponse with requestId set
     */
    public ApiResponse<T> withRequestId(String requestId) {
        return new ApiResponse<>(this.code, this.message, this.data, this.timestamp, requestId);
    }

    /**
     * Return a copy of this response with the timestamp removed.
     * <p>
     * Used when timestamp inclusion is disabled via configuration.
     *
     * @return new ApiResponse without timestamp
     */
    public ApiResponse<T> withoutTimestamp() {
        return new ApiResponse<>(this.code, this.message, this.data, null, this.requestId);
    }

    /**
     * Return a copy of this response with the request ID removed.
     * <p>
     * Used when request ID inclusion is disabled via configuration.
     *
     * @return new ApiResponse without requestId
     */
    public ApiResponse<T> withoutRequestId() {
        return new ApiResponse<>(this.code, this.message, this.data, this.timestamp, null);
    }

    /**
     * Return a copy of this response with a custom success code.
     *
     * @param successCode the custom success code
     * @return new ApiResponse with the success code applied
     */
    public ApiResponse<T> withSuccessCode(int successCode) {
        return new ApiResponse<>(successCode, this.message, this.data, this.timestamp, this.requestId);
    }

    /**
     * Return a copy of this response with a custom error code.
     *
     * @param errorCode the custom error code
     * @return new ApiResponse with the error code applied
     */
    public ApiResponse<T> withErrorCode(int errorCode) {
        return new ApiResponse<>(errorCode, this.message, this.data, this.timestamp, this.requestId);
    }

    // ======================================================================
    // Getters
    // ======================================================================

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Long getTimestamp() { return timestamp; }
    public String getRequestId() { return requestId; }

    // ======================================================================
    // equals / hashCode / toString
    // ======================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ApiResponse)) return false;
        ApiResponse<?> other = (ApiResponse<?>) obj;
        return code == other.code
                && Objects.equals(message, other.message)
                && Objects.equals(data, other.data)
                && Objects.equals(timestamp, other.timestamp)
                && Objects.equals(requestId, other.requestId);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(code);
        result = 31 * result + Objects.hashCode(message);
        result = 31 * result + Objects.hashCode(data);
        result = 31 * result + Objects.hashCode(timestamp);
        result = 31 * result + Objects.hashCode(requestId);
        return result;
    }

    @Override
    public String toString() {
        return "ApiResponse{code=" + code
                + ", message='" + message + '\''
                + ", data=" + data
                + ", timestamp=" + timestamp
                + ", requestId='" + requestId + '\''
                + '}';
    }
}
