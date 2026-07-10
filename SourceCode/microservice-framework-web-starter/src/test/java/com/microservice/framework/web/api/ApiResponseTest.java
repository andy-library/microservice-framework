package com.microservice.framework.web.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiResponse creation, factory methods and equality tests.
 *
 * @author Andy Yang
 */
class ApiResponseTest {

    // ======================================================================
    // Factory method tests
    // ======================================================================

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("success() 应返回 code=0, message='success', 无 data")
        void successEmpty() {
            ApiResponse<Void> response = ApiResponse.success();
            assertThat(response.getCode()).isEqualTo(0);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
            assertThat(response.getRequestId()).isNull();
        }

        @Test
        @DisplayName("success(data) 应返回 code=0, message='success', 含 data")
        void successWithData() {
            ApiResponse<String> response = ApiResponse.success("hello");
            assertThat(response.getCode()).isEqualTo(0);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo("hello");
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("success(message, data) 应返回 code=0, 自定义 message 和 data")
        void successWithMessageAndData() {
            ApiResponse<Integer> response = ApiResponse.success("created", 42);
            assertThat(response.getCode()).isEqualTo(0);
            assertThat(response.getMessage()).isEqualTo("created");
            assertThat(response.getData()).isEqualTo(42);
        }

        @Test
        @DisplayName("error(code, message) 应返回错误码和消息")
        void errorWithCodeAndMessage() {
            ApiResponse<Void> response = ApiResponse.error(-1, "something went wrong");
            assertThat(response.getCode()).isEqualTo(-1);
            assertThat(response.getMessage()).isEqualTo("something went wrong");
            assertThat(response.getData()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("error(code, message, data) 应返回错误码、消息和数据")
        void errorWithCodeMessageAndData() {
            ApiResponse<String> response = ApiResponse.error(-1, "conflict", "duplicate entry");
            assertThat(response.getCode()).isEqualTo(-1);
            assertThat(response.getMessage()).isEqualTo("conflict");
            assertThat(response.getData()).isEqualTo("duplicate entry");
        }

        @Test
        @DisplayName("of() 应设置所有字段")
        void ofAllFields() {
            long ts = 1700000000000L;
            ApiResponse<String> response = ApiResponse.of(1, "msg", "data", ts, "req-123");
            assertThat(response.getCode()).isEqualTo(1);
            assertThat(response.getMessage()).isEqualTo("msg");
            assertThat(response.getData()).isEqualTo("data");
            assertThat(response.getTimestamp()).isEqualTo(ts);
            assertThat(response.getRequestId()).isEqualTo("req-123");
        }
    }

    // ======================================================================
    // withXxx method tests
    // ======================================================================

    @Nested
    @DisplayName("withXxx 方法")
    class WithMethods {

        @Test
        @DisplayName("withRequestId 应注入 request ID")
        void withRequestId() {
            ApiResponse<String> original = ApiResponse.success("data");
            ApiResponse<String> modified = original.withRequestId("req-456");
            assertThat(modified.getRequestId()).isEqualTo("req-456");
            assertThat(modified.getCode()).isEqualTo(original.getCode());
            assertThat(modified.getMessage()).isEqualTo(original.getMessage());
            assertThat(modified.getData()).isEqualTo(original.getData());
        }

        @Test
        @DisplayName("withoutTimestamp 应移除 timestamp")
        void withoutTimestamp() {
            ApiResponse<String> original = ApiResponse.success("data");
            assertThat(original.getTimestamp()).isNotNull();
            ApiResponse<String> modified = original.withoutTimestamp();
            assertThat(modified.getTimestamp()).isNull();
            assertThat(modified.getData()).isEqualTo("data");
        }

        @Test
        @DisplayName("withoutRequestId 应移除 requestId")
        void withoutRequestId() {
            ApiResponse<String> original = ApiResponse.success("data").withRequestId("req-1");
            assertThat(original.getRequestId()).isEqualTo("req-1");
            ApiResponse<String> modified = original.withoutRequestId();
            assertThat(modified.getRequestId()).isNull();
        }

        @Test
        @DisplayName("withSuccessCode 应修改成功码")
        void withSuccessCode() {
            ApiResponse<String> original = ApiResponse.success("data");
            assertThat(original.getCode()).isEqualTo(0);
            ApiResponse<String> modified = original.withSuccessCode(200);
            assertThat(modified.getCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("withErrorCode 应修改错误码")
        void withErrorCode() {
            ApiResponse<String> original = ApiResponse.error(-1, "error");
            assertThat(original.getCode()).isEqualTo(-1);
            ApiResponse<String> modified = original.withErrorCode(500);
            assertThat(modified.getCode()).isEqualTo(500);
        }
    }

    // ======================================================================
    // Equality tests
    // ======================================================================

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("相同字段值的 ApiResponse 应相等")
        void equalResponses() {
            long ts = 1700000000000L;
            ApiResponse<String> r1 = ApiResponse.of(0, "success", "data", ts, "req-1");
            ApiResponse<String> r2 = ApiResponse.of(0, "success", "data", ts, "req-1");
            assertThat(r1).isEqualTo(r2);
            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("不同 code 的 ApiResponse 应不相等")
        void differentCodeNotEqual() {
            ApiResponse<String> r1 = ApiResponse.success("data");
            ApiResponse<String> r2 = ApiResponse.error(-1, "error", "data");
            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void comparedToNull() {
            ApiResponse<String> r = ApiResponse.success();
            assertThat(r).isNotEqualTo(null);
        }

        @Test
        @DisplayName("与不同类型比较应返回 false")
        void comparedToDifferentType() {
            ApiResponse<String> r = ApiResponse.success();
            assertThat(r).isNotEqualTo("not a response");
        }

        @Test
        @DisplayName("自身比较应返回 true")
        void selfEquality() {
            ApiResponse<String> r = ApiResponse.success();
            assertThat(r).isEqualTo(r);
        }
    }

    // ======================================================================
    // toString test
    // ======================================================================

    @Test
    @DisplayName("toString 应包含所有字段")
    void toStringShouldContainAllFields() {
        ApiResponse<String> r = ApiResponse.of(0, "ok", "data", 1700000000000L, "req-1");
        String str = r.toString();
        assertThat(str).contains("ApiResponse");
        assertThat(str).contains("code=0");
        assertThat(str).contains("message='ok'");
        assertThat(str).contains("data=data");
        assertThat(str).contains("requestId='req-1'");
    }
}
