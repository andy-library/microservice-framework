package com.microservice.framework.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebProperties default value tests.
 *
 * @author Andy Yang
 */
class WebPropertiesTest {

    private final WebProperties properties = new WebProperties();

    // ======================================================================
    // Response defaults
    // ======================================================================

    @Nested
    @DisplayName("Response 默认值")
    class ResponseDefaults {

        @Test
        @DisplayName("successCode 默认应为 0")
        void successCodeDefault() {
            assertThat(properties.getResponse().getSuccessCode()).isEqualTo(0);
        }

        @Test
        @DisplayName("errorCode 默认应为 -1")
        void errorCodeDefault() {
            assertThat(properties.getResponse().getErrorCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("includeTimestamp 默认应为 true")
        void includeTimestampDefault() {
            assertThat(properties.getResponse().isIncludeTimestamp()).isTrue();
        }

        @Test
        @DisplayName("includeRequestId 默认应为 true")
        void includeRequestIdDefault() {
            assertThat(properties.getResponse().isIncludeRequestId()).isTrue();
        }
    }

    // ======================================================================
    // Exception defaults
    // ======================================================================

    @Nested
    @DisplayName("Exception 默认值")
    class ExceptionDefaults {

        @Test
        @DisplayName("enabled 默认应为 true")
        void enabledDefault() {
            assertThat(properties.getException().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("includeStackTrace 默认应为 false")
        void includeStackTraceDefault() {
            assertThat(properties.getException().isIncludeStackTrace()).isFalse();
        }
    }

    // ======================================================================
    // RequestId defaults
    // ======================================================================

    @Nested
    @DisplayName("RequestId 默认值")
    class RequestIdDefaults {

        @Test
        @DisplayName("enabled 默认应为 true")
        void enabledDefault() {
            assertThat(properties.getRequestId().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("headerName 默认应为 'X-Request-ID'")
        void headerNameDefault() {
            assertThat(properties.getRequestId().getHeaderName()).isEqualTo("X-Request-ID");
        }

        @Test
        @DisplayName("generateIfMissing 默认应为 true")
        void generateIfMissingDefault() {
            assertThat(properties.getRequestId().isGenerateIfMissing()).isTrue();
        }
    }

    // ======================================================================
    // OpenApi defaults
    // ======================================================================

    @Nested
    @DisplayName("OpenApi 默认值")
    class OpenApiDefaults {

        @Test
        @DisplayName("enabled 默认应为 false")
        void enabledDefault() {
            assertThat(properties.getOpenApi().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("title 默认应为 null")
        void titleDefault() {
            assertThat(properties.getOpenApi().getTitle()).isNull();
        }

        @Test
        @DisplayName("version 默认应为 null")
        void versionDefault() {
            assertThat(properties.getOpenApi().getVersion()).isNull();
        }
    }

    // ======================================================================
    // Setter overrides
    // ======================================================================

    @Nested
    @DisplayName("Setter 覆盖")
    class SetterOverrides {

        @Test
        @DisplayName("自定义 successCode 应生效")
        void customSuccessCode() {
            properties.getResponse().setSuccessCode(200);
            assertThat(properties.getResponse().getSuccessCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("自定义 errorCode 应生效")
        void customErrorCode() {
            properties.getResponse().setErrorCode(500);
            assertThat(properties.getResponse().getErrorCode()).isEqualTo(500);
        }

        @Test
        @DisplayName("自定义 headerName 应生效")
        void customHeaderName() {
            properties.getRequestId().setHeaderName("X-Custom-ID");
            assertThat(properties.getRequestId().getHeaderName()).isEqualTo("X-Custom-ID");
        }

        @Test
        @DisplayName("OpenApi enabled 设为 true 应生效")
        void openApiEnabled() {
            properties.getOpenApi().setEnabled(true);
            properties.getOpenApi().setTitle("My API");
            properties.getOpenApi().setVersion("1.0.0");
            assertThat(properties.getOpenApi().isEnabled()).isTrue();
            assertThat(properties.getOpenApi().getTitle()).isEqualTo("My API");
            assertThat(properties.getOpenApi().getVersion()).isEqualTo("1.0.0");
        }
    }
}
