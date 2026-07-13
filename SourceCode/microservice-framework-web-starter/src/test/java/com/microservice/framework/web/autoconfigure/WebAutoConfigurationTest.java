package com.microservice.framework.web.autoconfigure;

import com.microservice.framework.web.WebProperties;
import com.microservice.framework.web.exception.GlobalExceptionHandler;
import com.microservice.framework.web.context.RequestIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebAutoConfiguration ApplicationContextRunner tests.
 * <p>
 * Uses {@link WebApplicationContextRunner} (not ApplicationContextRunner)
 * to simulate SERVLET web application conditions.
 *
 * @author Andy Yang
 */
class WebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    WebAutoConfiguration.class,
                    ExceptionHandlerAutoConfiguration.class,
                    RequestIdAutoConfiguration.class));

    // ======================================================================
    // Default activation
    // ======================================================================

    @Nested
    @DisplayName("默认配置激活")
    class DefaultActivation {

        @Test
        @DisplayName("默认配置应激活所有自动配置")
        void defaultConfigurationShouldActivateAll() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("globalExceptionHandler");
                assertThat(context).hasBean("requestIdFilter");
                assertThat(context).hasBean("responseWrappingAdvice");
                assertThat(context).hasSingleBean(ResponseBodyAdvice.class);
                assertThat(context.getBean(WebProperties.class)).isNotNull();
            });
        }
    }

    // ======================================================================
    // Exception handler conditional
    // ======================================================================

    @Nested
    @DisplayName("异常处理条件激活")
    class ExceptionHandlerConditional {

        @Test
        @DisplayName("禁用 exception.enabled 后 GlobalExceptionHandler 不应存在")
        void disablingExceptionShouldRemoveHandler() {
            contextRunner.withPropertyValues("framework.web.exception.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("globalExceptionHandler");
                        assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class);
                    });
        }
    }

    // ======================================================================
    // Request ID conditional
    // ======================================================================

    @Nested
    @DisplayName("RequestId 条件激活")
    class RequestIdConditional {

        @Test
        @DisplayName("禁用 request-id.enabled 后 RequestIdFilter 不应存在")
        void disablingRequestIdShouldRemoveFilter() {
            contextRunner.withPropertyValues("framework.web.request-id.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("requestIdFilter");
                        assertThat(context).doesNotHaveBean(RequestIdFilter.class);
                    });
        }
    }

    // ======================================================================
    // Property binding
    // ======================================================================

    @Nested
    @DisplayName("属性绑定")
    class PropertyBinding {

        @Test
        @DisplayName("自定义 errorCode 应绑定到 WebProperties")
        void customErrorCodeBinding() {
            contextRunner.withPropertyValues("framework.web.response.error-code=500")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        WebProperties props = context.getBean(WebProperties.class);
                        assertThat(props.getResponse().getErrorCode()).isEqualTo(500);
                    });
        }

        @Test
        @DisplayName("自定义 successCode 应绑定到 WebProperties")
        void customSuccessCodeBinding() {
            contextRunner.withPropertyValues("framework.web.response.success-code=200")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        WebProperties props = context.getBean(WebProperties.class);
                        assertThat(props.getResponse().getSuccessCode()).isEqualTo(200);
                    });
        }

        @Test
        @DisplayName("自定义 headerName 应绑定到 WebProperties")
        void customHeaderNameBinding() {
            contextRunner.withPropertyValues("framework.web.request-id.header-name=X-Custom-ID")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        WebProperties props = context.getBean(WebProperties.class);
                        assertThat(props.getRequestId().getHeaderName()).isEqualTo("X-Custom-ID");
                    });
        }

        @Test
        @DisplayName("includeStackTrace=true 应绑定到 WebProperties")
        void includeStackTraceBinding() {
            contextRunner.withPropertyValues("framework.web.exception.include-stack-trace=true")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        WebProperties props = context.getBean(WebProperties.class);
                        assertThat(props.getException().isIncludeStackTrace()).isTrue();
                    });
        }

        @Test
        @DisplayName("includeTimestamp=false 应绑定到 WebProperties")
        void includeTimestampBinding() {
            contextRunner.withPropertyValues("framework.web.response.include-timestamp=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        WebProperties props = context.getBean(WebProperties.class);
                        assertThat(props.getResponse().isIncludeTimestamp()).isFalse();
                    });
        }
    }

    // ======================================================================
    // User bean override
    // ======================================================================

    @Nested
    @DisplayName("用户自定义 Bean 覆盖")
    class UserBeanOverride {

        @Test
        @DisplayName("用户提供的 GlobalExceptionHandler 应覆盖默认 Bean")
        void userProvidedExceptionHandlerShouldOverrideDefault() {
            WebProperties props = new WebProperties();
            GlobalExceptionHandler customHandler = new GlobalExceptionHandler(props);

            contextRunner.withBean("customExceptionHandler", GlobalExceptionHandler.class, () -> customHandler)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customExceptionHandler");
                        assertThat(context).doesNotHaveBean("globalExceptionHandler");
                        assertThat(context.getBean(GlobalExceptionHandler.class)).isSameAs(customHandler);
                    });
        }

        @Test
        @DisplayName("用户提供的 RequestIdFilter 应覆盖默认 Bean")
        void userProvidedRequestIdFilterShouldOverrideDefault() {
            WebProperties.RequestIdProperties reqProps = new WebProperties.RequestIdProperties();
            RequestIdFilter customFilter = new RequestIdFilter(reqProps);

            contextRunner.withBean("customRequestIdFilter", RequestIdFilter.class, () -> customFilter)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customRequestIdFilter");
                        assertThat(context).doesNotHaveBean("requestIdFilter");
                        assertThat(context.getBean(RequestIdFilter.class)).isSameAs(customFilter);
                    });
        }
    }
}
