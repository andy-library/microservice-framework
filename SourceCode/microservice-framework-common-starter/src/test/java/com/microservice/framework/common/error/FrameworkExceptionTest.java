package com.microservice.framework.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FrameworkException 单元测试
 * <p>
 * 验证异常的构造、消息格式以及错误码获取。
 *
 * @author Andy Yang
 */
class FrameworkExceptionTest {

    @Test
    @DisplayName("仅错误码构造时消息应为错误码字符串")
    void constructorWithErrorCodeOnlyShouldUseCodeAsMessage() {
        FrameworkErrorCode errorCode = FrameworkErrorCode.COMMON_TIME_INVALID_ZONE;
        FrameworkException exception = new FrameworkException(errorCode);

        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.code());
    }

    @Test
    @DisplayName("错误码 + 消息构造时消息应包含 [CODE] 前缀")
    void constructorWithErrorCodeAndMessageShouldIncludeCodePrefix() {
        FrameworkErrorCode errorCode = FrameworkErrorCode.COMMON_TIME_INVALID_ZONE;
        String detailMessage = "ZoneId must not be null";
        FrameworkException exception = new FrameworkException(errorCode, detailMessage);

        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo("[COMMON-TIME-001] ZoneId must not be null");
    }

    @Test
    @DisplayName("错误码 + 消息 + 原因构造时应保留完整信息")
    void constructorWithErrorCodeMessageAndCauseShouldPreserveAll() {
        FrameworkErrorCode errorCode = FrameworkErrorCode.COMMON_TIME_INVALID_ZONE;
        String detailMessage = "Invalid time zone";
        RuntimeException cause = new RuntimeException("original cause");
        FrameworkException exception = new FrameworkException(errorCode, detailMessage, cause);

        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo("[COMMON-TIME-001] Invalid time zone");
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("original cause");
    }

    @Test
    @DisplayName("错误码 + 原因构造时消息应为错误码字符串")
    void constructorWithErrorCodeAndCauseShouldUseCodeAsMessage() {
        FrameworkErrorCode errorCode = FrameworkErrorCode.COMMON_ID_CLOCK_BACKWARD;
        RuntimeException cause = new RuntimeException("clock issue");
        FrameworkException exception = new FrameworkException(errorCode, cause);

        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.code());
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("getErrorCode() 应返回传入的错误码")
    void getErrorCodeShouldReturnProvidedErrorCode() {
        FrameworkErrorCode errorCode = FrameworkErrorCode.COMMON_ID_WORKER_INVALID;
        FrameworkException exception = new FrameworkException(errorCode);
        assertThat(exception.getErrorCode()).isSameAs(errorCode);
    }
}
