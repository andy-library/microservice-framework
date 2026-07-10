package com.microservice.framework.web.exception;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;
import com.microservice.framework.web.api.ApiResponse;
import com.microservice.framework.web.api.ErrorCodeResponse;
import com.microservice.framework.web.context.RequestIdContext;
import com.microservice.framework.web.WebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler exception handling tests.
 *
 * @author Andy Yang
 */
class GlobalExceptionHandlerTest {

    private final WebProperties properties = createDefaultProperties();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(properties);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    private static WebProperties createDefaultProperties() {
        WebProperties props = new WebProperties();
        props.getResponse().setSuccessCode(0);
        props.getResponse().setErrorCode(-1);
        props.getResponse().setIncludeTimestamp(true);
        props.getResponse().setIncludeRequestId(true);
        props.getException().setEnabled(true);
        props.getException().setIncludeStackTrace(false);
        props.getRequestId().setEnabled(true);
        props.getRequestId().setHeaderName("X-Request-ID");
        props.getRequestId().setGenerateIfMissing(true);
        return props;
    }

    // ======================================================================
    // FrameworkException handling
    // ======================================================================

    @Nested
    @DisplayName("FrameworkException 处理")
    class FrameworkExceptionHandling {

        @Test
        @DisplayName("FrameworkException 应返回 400 和错误响应")
        void handleFrameworkException() {
            FrameworkException ex = new FrameworkException(
                    FrameworkErrorCode.of("WEB", "BIZ", 1), "business error");

            ResponseEntity<ApiResponse<Object>> result = handler.handleFrameworkException(ex, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ApiResponse<Object> body = result.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getCode()).isEqualTo(-1);
            assertThat(body.getMessage()).contains("WEB-BIZ-001");
            assertThat(body.getMessage()).contains("business error");
        }

        @Test
        @DisplayName("FrameworkException 仅 errorCode 时消息应为错误码")
        void frameworkExceptionWithOnlyErrorCode() {
            FrameworkException ex = new FrameworkException(
                    BusinessException.WEB_BIZ_NOT_FOUND);

            ResponseEntity<ApiResponse<Object>> result = handler.handleFrameworkException(ex, request);

            assertThat(result.getBody().getMessage()).isEqualTo("WEB-BIZ-002");
        }
    }

    // ======================================================================
    // ValidationException handling
    // ======================================================================

    @Nested
    @DisplayName("ValidationException 处理")
    class ValidationExceptionHandling {

        @Test
        @DisplayName("ValidationException 应返回 400 和 ErrorCodeResponse")
        void handleValidationException() {
            ValidationException ex = new ValidationException(
                    ValidationException.WEB_VALID_GENERAL,
                    List.of(new ValidationException.FieldError("name", "must not be blank")));

            ResponseEntity<ErrorCodeResponse> result = handler.handleValidationException(ex, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ErrorCodeResponse body = result.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getCode()).isEqualTo(-1);
            assertThat(body.getErrorCode()).isEqualTo("WEB-VALID-001");
            assertThat(body.getDetails()).containsEntry("name", "must not be blank");
        }

        @Test
        @DisplayName("includeStackTrace=true 时 ErrorCodeResponse 应含 stackTrace")
        void validationExceptionWithStackTrace() {
            WebProperties props = createDefaultProperties();
            props.getException().setIncludeStackTrace(true);
            GlobalExceptionHandler handlerWithTrace = new GlobalExceptionHandler(props);

            ValidationException ex = new ValidationException(ValidationException.WEB_VALID_GENERAL, "validation failed");

            ResponseEntity<ErrorCodeResponse> result = handlerWithTrace.handleValidationException(ex, request);

            assertThat(result.getBody().getStackTrace()).isNotNull();
        }

        @Test
        @DisplayName("includeStackTrace=false 时 ErrorCodeResponse 不应含 stackTrace")
        void validationExceptionWithoutStackTrace() {
            ValidationException ex = new ValidationException(ValidationException.WEB_VALID_GENERAL, "validation failed");

            ResponseEntity<ErrorCodeResponse> result = handler.handleValidationException(ex, request);

            assertThat(result.getBody().getStackTrace()).isNull();
        }
    }

    // ======================================================================
    // MethodArgumentNotValidException handling
    // ======================================================================

    @Nested
    @DisplayName("MethodArgumentNotValidException 处理")
    class MethodArgumentNotValidHandling {

        @Test
        @DisplayName("MethodArgumentNotValidException 应返回 400 和字段错误")
        void handleMethodArgumentNotValid() throws NoSuchMethodException {
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
            bindingResult.addError(new FieldError("obj", "email", "must be a valid email"));
            bindingResult.addError(new FieldError("obj", "name", "must not be blank"));
            Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", String.class);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                    new MethodParameter(method, 0), bindingResult);

            ResponseEntity<ErrorCodeResponse> result = handler.handleMethodArgumentNotValid(ex, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ErrorCodeResponse body = result.getBody();
            assertThat(body.getCode()).isEqualTo(-1);
            assertThat(body.getMessage()).isEqualTo("Validation failed");
            assertThat(body.getDetails()).containsEntry("email", "must be a valid email");
            assertThat(body.getDetails()).containsEntry("name", "must not be blank");
        }
    }

    @SuppressWarnings("unused")
    private static void validationTarget(String value) {
    }

    // ======================================================================
    // ConstraintViolationException handling
    // ======================================================================

    @Nested
    @DisplayName("ConstraintViolationException 处理")
    class ConstraintViolationHandling {

        @Test
        @DisplayName("ConstraintViolationException 应返回 400 和字段错误")
        void handleConstraintViolation() {
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn("age");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("must be positive");

            Set<ConstraintViolation<?>> violations = new HashSet<>();
            violations.add(violation);
            ConstraintViolationException ex = new ConstraintViolationException(violations);

            ResponseEntity<ErrorCodeResponse> result = handler.handleConstraintViolation(ex, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(result.getBody().getDetails()).containsEntry("age", "must be positive");
        }
    }

    // ======================================================================
    // Generic Exception catch-all
    // ======================================================================

    @Nested
    @DisplayName("Generic Exception 处理")
    class GenericExceptionHandling {

        @Test
        @DisplayName("未知异常应返回 500 和通用错误消息")
        void handleGenericException() {
            Exception ex = new RuntimeException("unexpected error");

            ResponseEntity<ApiResponse<Object>> result = handler.handleGenericException(ex, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            ApiResponse<Object> body = result.getBody();
            assertThat(body.getCode()).isEqualTo(-1);
            assertThat(body.getMessage()).isEqualTo("Internal server error");
        }

        @Test
        @DisplayName("includeStackTrace=true 时通用异常应含类名和消息")
        void genericExceptionWithStackTraceEnabled() {
            WebProperties props = createDefaultProperties();
            props.getException().setIncludeStackTrace(true);
            GlobalExceptionHandler handlerWithTrace = new GlobalExceptionHandler(props);

            Exception ex = new RuntimeException("unexpected error");

            ResponseEntity<ApiResponse<Object>> result = handlerWithTrace.handleGenericException(ex, request);

            assertThat(result.getBody().getMessage()).contains("RuntimeException");
            assertThat(result.getBody().getMessage()).contains("unexpected error");
        }
    }

    // ======================================================================
    // Request ID propagation
    // ======================================================================

    @Nested
    @DisplayName("Request ID 传播")
    class RequestIdPropagation {

        @Test
        @DisplayName("RequestIdContext 有值时应注入到响应")
        void requestIdFromContext() {
            RequestIdContext.set("ctx-req-123");
            try {
                FrameworkException ex = new FrameworkException(
                        FrameworkErrorCode.of("WEB", "BIZ", 1), "error");

                ResponseEntity<ApiResponse<Object>> result = handler.handleFrameworkException(ex, request);

                assertThat(result.getBody().getRequestId()).isEqualTo("ctx-req-123");
            } finally {
                RequestIdContext.remove();
            }
        }

        @Test
        @DisplayName("RequestIdContext 为空时应从 header 取值")
        void requestIdFromHeader() {
            when(request.getHeader("X-Request-ID")).thenReturn("header-req-456");

            FrameworkException ex = new FrameworkException(
                    FrameworkErrorCode.of("WEB", "BIZ", 1), "error");

            ResponseEntity<ApiResponse<Object>> result = handler.handleFrameworkException(ex, request);

            assertThat(result.getBody().getRequestId()).isEqualTo("header-req-456");
        }

        @Test
        @DisplayName("includeRequestId=false 时 requestId 应为 null")
        void requestIdDisabled() {
            WebProperties props = createDefaultProperties();
            props.getResponse().setIncludeRequestId(false);
            GlobalExceptionHandler disabledHandler = new GlobalExceptionHandler(props);
            RequestIdContext.set("should-not-appear");

            try {
                FrameworkException ex = new FrameworkException(
                        FrameworkErrorCode.of("WEB", "BIZ", 1), "error");

                ResponseEntity<ApiResponse<Object>> result = disabledHandler.handleFrameworkException(ex, request);

                assertThat(result.getBody().getRequestId()).isNull();
            } finally {
                RequestIdContext.remove();
            }
        }

        @Test
        @DisplayName("includeTimestamp=false 时 timestamp 应为 null")
        void timestampDisabled() {
            WebProperties props = createDefaultProperties();
            props.getResponse().setIncludeTimestamp(false);
            GlobalExceptionHandler disabledHandler = new GlobalExceptionHandler(props);

            FrameworkException ex = new FrameworkException(
                    FrameworkErrorCode.of("WEB", "BIZ", 1), "error");

            ResponseEntity<ApiResponse<Object>> result = disabledHandler.handleFrameworkException(ex, request);

            assertThat(result.getBody().getTimestamp()).isNull();
        }
    }

    // ======================================================================
    // Custom error/success codes
    // ======================================================================

    @Nested
    @DisplayName("自定义错误码配置")
    class CustomErrorCodeConfig {

        @Test
        @DisplayName("自定义 errorCode=500 时响应应使用该值")
        void customErrorCode() {
            WebProperties props = createDefaultProperties();
            props.getResponse().setErrorCode(500);
            GlobalExceptionHandler customHandler = new GlobalExceptionHandler(props);

            FrameworkException ex = new FrameworkException(
                    FrameworkErrorCode.of("WEB", "BIZ", 1), "error");

            ResponseEntity<ApiResponse<Object>> result = customHandler.handleFrameworkException(ex, request);

            assertThat(result.getBody().getCode()).isEqualTo(500);
        }
    }
}
