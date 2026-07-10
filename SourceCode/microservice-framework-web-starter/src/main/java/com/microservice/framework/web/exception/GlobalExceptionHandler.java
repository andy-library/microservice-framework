package com.microservice.framework.web.exception;

import com.microservice.framework.common.error.FrameworkException;
import com.microservice.framework.web.api.ApiResponse;
import com.microservice.framework.web.api.ErrorCodeResponse;
import com.microservice.framework.web.context.RequestIdContext;
import com.microservice.framework.web.WebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Web module.
 * <p>
 * Catches all exceptions thrown within the request processing pipeline
 * and converts them to structured {@link ApiResponse} / {@link ErrorCodeResponse}
 * responses with appropriate HTTP status codes.
 * <p>
 * Handles:
 * <ul>
 *   <li>{@link FrameworkException} — structured error code + message</li>
 *   <li>{@link ValidationException} — field-level validation errors</li>
 *   <li>{@link MethodArgumentNotValidException} — Spring MVC validation</li>
 *   <li>{@link ConstraintViolationException} — Jakarta Validation</li>
 *   <li>{@link Exception} — catch-all for unexpected errors</li>
 * </ul>
 *
 * @author Andy Yang
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final WebProperties properties;

    public GlobalExceptionHandler(WebProperties properties) {
        this.properties = properties;
    }

    // ======================================================================
    // FrameworkException handling
    // ======================================================================

    @ExceptionHandler(FrameworkException.class)
    public ResponseEntity<ApiResponse<Object>> handleFrameworkException(
            FrameworkException ex, HttpServletRequest request) {
        log.warn("Framework exception: {}", ex.getMessage(), ex);

        WebProperties.ExceptionProperties exceptionProps = properties.getException();
        WebProperties.ResponseProperties responseProps = properties.getResponse();

        String requestId = resolveRequestId(request);
        Long timestamp = responseProps.isIncludeTimestamp() ? System.currentTimeMillis() : null;

        ApiResponse<Object> response = ApiResponse.of(
                responseProps.getErrorCode(),
                ex.getMessage(),
                null,
                timestamp,
                requestId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ======================================================================
    // ValidationException handling
    // ======================================================================

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorCodeResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        log.warn("Validation exception: {}", ex.getMessage());

        WebProperties.ExceptionProperties exceptionProps = properties.getException();
        WebProperties.ResponseProperties responseProps = properties.getResponse();

        String requestId = resolveRequestId(request);
        Long timestamp = responseProps.isIncludeTimestamp() ? System.currentTimeMillis() : null;

        Map<String, String> details = ex.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.field(),
                        fe -> fe.message(),
                        (existing, replacement) -> existing
                ));

        String stackTrace = exceptionProps.isIncludeStackTrace()
                ? getStackTraceAsString(ex) : null;

        ErrorCodeResponse response = ErrorCodeResponse.of(
                responseProps.getErrorCode(),
                ex.getMessage(),
                ex.getErrorCode().code(),
                details,
                stackTrace,
                timestamp,
                requestId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ======================================================================
    // MethodArgumentNotValidException handling (Spring MVC @Valid)
    // ======================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorCodeResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Method argument validation failed: {}", ex.getMessage());

        WebProperties.ExceptionProperties exceptionProps = properties.getException();
        WebProperties.ResponseProperties responseProps = properties.getResponse();

        String requestId = resolveRequestId(request);
        Long timestamp = responseProps.isIncludeTimestamp() ? System.currentTimeMillis() : null;

        Map<String, String> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String stackTrace = exceptionProps.isIncludeStackTrace()
                ? getStackTraceAsString(ex) : null;

        ErrorCodeResponse response = ErrorCodeResponse.of(
                responseProps.getErrorCode(),
                "Validation failed",
                ValidationException.WEB_VALID_GENERAL.code(),
                details,
                stackTrace,
                timestamp,
                requestId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ======================================================================
    // ConstraintViolationException handling (Jakarta Validation)
    // ======================================================================

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorCodeResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());

        WebProperties.ExceptionProperties exceptionProps = properties.getException();
        WebProperties.ResponseProperties responseProps = properties.getResponse();

        String requestId = resolveRequestId(request);
        Long timestamp = responseProps.isIncludeTimestamp() ? System.currentTimeMillis() : null;

        Map<String, String> details = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (existing, replacement) -> existing
                ));

        String stackTrace = exceptionProps.isIncludeStackTrace()
                ? getStackTraceAsString(ex) : null;

        ErrorCodeResponse response = ErrorCodeResponse.of(
                responseProps.getErrorCode(),
                "Validation failed",
                ValidationException.WEB_VALID_GENERAL.code(),
                details,
                stackTrace,
                timestamp,
                requestId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ======================================================================
    // Generic Exception catch-all
    // ======================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);

        WebProperties.ExceptionProperties exceptionProps = properties.getException();
        WebProperties.ResponseProperties responseProps = properties.getResponse();

        String requestId = resolveRequestId(request);
        Long timestamp = responseProps.isIncludeTimestamp() ? System.currentTimeMillis() : null;

        String message = exceptionProps.isIncludeStackTrace()
                ? ex.getClass().getName() + ": " + ex.getMessage()
                : "Internal server error";

        ApiResponse<Object> response = ApiResponse.of(
                responseProps.getErrorCode(),
                message,
                null,
                timestamp,
                requestId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private String resolveRequestId(HttpServletRequest request) {
        if (!properties.getResponse().isIncludeRequestId()) {
            return null;
        }
        // Try RequestIdContext first, then fall back to header
        String requestId = RequestIdContext.get();
        if (requestId == null) {
            requestId = request.getHeader(properties.getRequestId().getHeaderName());
        }
        return requestId;
    }

    private String getStackTraceAsString(Throwable ex) {
        StackTraceElement[] elements = ex.getStackTrace();
        if (elements == null || elements.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(elements.length, 10); i++) {
            sb.append(elements[i].toString()).append('\n');
        }
        return sb.toString();
    }
}
