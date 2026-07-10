package com.microservice.framework.json.api;

import com.microservice.framework.common.error.FrameworkErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON 编解码异常测试
 * <p>
 * 验证 {@link JsonCodecException} 的错误码格式、消息格式和异常链传播。
 *
 * @author Andy Yang
 */
class JsonCodecExceptionTest {

    // ======================================================================
     // 错误码格式测试
     // ======================================================================

    @Nested
    @DisplayName("错误码格式")
    class ErrorCodeFormat {

        @Test
        @DisplayName("JSON_PARAM_NULL 错误码格式应为 JSON-PARAM-001")
        void paramNullErrorCode() {
            assertThat(JsonCodecException.JSON_PARAM_NULL.code()).isEqualTo("JSON-PARAM-001");
        }

        @Test
        @DisplayName("JSON_FORMAT_ERROR 错误码格式应为 JSON-FORMAT-001")
        void formatErrorCode() {
            assertThat(JsonCodecException.JSON_FORMAT_ERROR.code()).isEqualTo("JSON-FORMAT-001");
        }

        @Test
        @DisplayName("JSON_TYPE_MISMATCH 错误码格式应为 JSON-TYPE-001")
        void typeMismatchErrorCode() {
            assertThat(JsonCodecException.JSON_TYPE_MISMATCH.code()).isEqualTo("JSON-TYPE-001");
        }

        @Test
        @DisplayName("JSON_POLYMORPHIC_BLOCKED 错误码格式应为 JSON-SECURITY-001")
        void polymorphicBlockedErrorCode() {
            assertThat(JsonCodecException.JSON_POLYMORPHIC_BLOCKED.code()).isEqualTo("JSON-SECURITY-001");
        }

        @Test
        @DisplayName("JSON_DEPTH_EXCEEDED 错误码格式应为 JSON-SECURITY-002")
        void depthExceededErrorCode() {
            assertThat(JsonCodecException.JSON_DEPTH_EXCEEDED.code()).isEqualTo("JSON-SECURITY-002");
        }

        @Test
        @DisplayName("JSON_PAYLOAD_EXCEEDED 错误码格式应为 JSON-SECURITY-003")
        void payloadExceededErrorCode() {
            assertThat(JsonCodecException.JSON_PAYLOAD_EXCEEDED.code()).isEqualTo("JSON-SECURITY-003");
        }

        @Test
        @DisplayName("JSON_UNKNOWN_PROPERTY 错误码格式应为 JSON-DESER-001")
        void unknownPropertyErrorCode() {
            assertThat(JsonCodecException.JSON_UNKNOWN_PROPERTY.code()).isEqualTo("JSON-DESER-001");
        }

        @Test
        @DisplayName("JSON_INTERNAL_ERROR 错误码格式应为 JSON-INTERNAL-001")
        void internalErrorCode() {
            assertThat(JsonCodecException.JSON_INTERNAL_ERROR.code()).isEqualTo("JSON-INTERNAL-001");
        }
    }

    // ======================================================================
     // 异常消息格式测试
     // ======================================================================

    @Nested
    @DisplayName("异常消息格式")
    class MessageFormat {

        @Test
        @DisplayName("仅错误码时消息应为错误码本身")
        void messageWithOnlyErrorCode() {
            JsonCodecException ex = new JsonCodecException(JsonCodecException.JSON_FORMAT_ERROR);
            assertThat(ex.getMessage()).isEqualTo("JSON-FORMAT-001");
        }

        @Test
        @DisplayName("带详细信息时消息格式应为 [CODE] detail")
        void messageWithDetail() {
            JsonCodecException ex = new JsonCodecException(
                    JsonCodecException.JSON_PARAM_NULL, "value must not be null");
            assertThat(ex.getMessage()).isEqualTo("[JSON-PARAM-001] value must not be null");
        }

        @Test
        @DisplayName("带原始异常时应正确传播 cause")
        void messageWithCause() {
            RuntimeException cause = new RuntimeException("original error");
            JsonCodecException ex = new JsonCodecException(
                    JsonCodecException.JSON_INTERNAL_ERROR, "operation failed", cause);
            assertThat(ex.getMessage()).isEqualTo("[JSON-INTERNAL-001] operation failed");
            assertThat(ex.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("仅错误码和原始异常时应传播 cause")
        void errorCodeAndCause() {
            RuntimeException cause = new RuntimeException("original");
            JsonCodecException ex = new JsonCodecException(JsonCodecException.JSON_FORMAT_ERROR, cause);
            assertThat(ex.getCause()).isEqualTo(cause);
        }
    }

    // ======================================================================
     // FrameworkErrorCode 属性测试
     // ======================================================================

    @Nested
    @DisplayName("FrameworkErrorCode 属性")
    class ErrorCodeProperties {

        @Test
        @DisplayName("getErrorCode 应返回正确的 FrameworkErrorCode")
        void shouldReturnErrorCode() {
            JsonCodecException ex = new JsonCodecException(JsonCodecException.JSON_DEPTH_EXCEEDED);
            FrameworkErrorCode errorCode = ex.getErrorCode();
            assertThat(errorCode).isEqualTo(JsonCodecException.JSON_DEPTH_EXCEEDED);
        }

        @Test
        @DisplayName("错误码的 module 应为 JSON")
        void moduleShouldBeJson() {
            assertThat(JsonCodecException.JSON_PARAM_NULL.getModule()).isEqualTo("JSON");
        }

        @Test
        @DisplayName("错误码的 category 应正确区分不同类别")
        void categoryShouldDistinguishCategories() {
            assertThat(JsonCodecException.JSON_PARAM_NULL.getCategory()).isEqualTo("PARAM");
            assertThat(JsonCodecException.JSON_FORMAT_ERROR.getCategory()).isEqualTo("FORMAT");
            assertThat(JsonCodecException.JSON_DEPTH_EXCEEDED.getCategory()).isEqualTo("SECURITY");
            assertThat(JsonCodecException.JSON_UNKNOWN_PROPERTY.getCategory()).isEqualTo("DESER");
            assertThat(JsonCodecException.JSON_INTERNAL_ERROR.getCategory()).isEqualTo("INTERNAL");
        }
    }
}
