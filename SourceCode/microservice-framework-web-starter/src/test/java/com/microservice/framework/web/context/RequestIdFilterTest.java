package com.microservice.framework.web.context;

import com.microservice.framework.web.WebProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * RequestIdFilter behavior tests.
 * <p>
 * Since the filter sets RequestIdContext before calling chain.doFilter()
 * and removes it in the finally block, we verify the request ID
 * by capturing it during the filter chain execution.
 *
 * @author Andy Yang
 */
class RequestIdFilterTest {

    private final WebProperties.RequestIdProperties defaultProps = new WebProperties.RequestIdProperties();
    private final RequestIdFilter filter = new RequestIdFilter(defaultProps);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final ServletResponse response = mock(ServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);

    // ======================================================================
    // Existing header propagation
    // ======================================================================

    @Nested
    @DisplayName("已有 request ID header 传播")
    class ExistingHeaderPropagation {

        @Test
        @DisplayName("已有 X-Request-ID header 时应使用该值")
        void useExistingRequestId() throws Exception {
            when(request.getHeader("X-Request-ID")).thenReturn("existing-req-123");

            AtomicReference<String> capturedId = new AtomicReference<>();
            org.mockito.Mockito.doAnswer(invocation -> {
                capturedId.set(RequestIdContext.get());
                return null;
            }).when(chain).doFilter(request, response);

            filter.doFilter(request, response, chain);

            assertThat(capturedId.get()).isEqualTo("existing-req-123");
            assertThat(RequestIdContext.get()).isNull(); // cleaned up after filter
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("空白 header 应视为不存在并生成新 ID")
        void blankHeaderShouldGenerateNew() throws Exception {
            when(request.getHeader("X-Request-ID")).thenReturn("   ");

            AtomicReference<String> capturedId = new AtomicReference<>();
            org.mockito.Mockito.doAnswer(invocation -> {
                capturedId.set(RequestIdContext.get());
                return null;
            }).when(chain).doFilter(request, response);

            filter.doFilter(request, response, chain);

            assertThat(capturedId.get()).isNotNull();
            assertThat(capturedId.get()).isNotBlank();
        }
    }

    // ======================================================================
    // Missing header generation
    // ======================================================================

    @Nested
    @DisplayName("缺失 header 时生成 request ID")
    class MissingHeaderGeneration {

        @Test
        @DisplayName("缺失 header 且 generateIfMissing=true 时应生成新 ID")
        void generateWhenMissingAndEnabled() throws Exception {
            when(request.getHeader("X-Request-ID")).thenReturn(null);

            AtomicReference<String> capturedId = new AtomicReference<>();
            org.mockito.Mockito.doAnswer(invocation -> {
                capturedId.set(RequestIdContext.get());
                return null;
            }).when(chain).doFilter(request, response);

            filter.doFilter(request, response, chain);

            assertThat(capturedId.get()).isNotNull();
            assertThat(capturedId.get()).isNotBlank();
            assertThat(capturedId.get().length()).isEqualTo(32); // UUID without dashes
        }

        @Test
        @DisplayName("缺失 header 且 generateIfMissing=false 时不应生成 ID")
        void noGenerateWhenDisabled() throws Exception {
            WebProperties.RequestIdProperties props = new WebProperties.RequestIdProperties();
            props.setGenerateIfMissing(false);
            RequestIdFilter disabledFilter = new RequestIdFilter(props);

            when(request.getHeader("X-Request-ID")).thenReturn(null);

            AtomicReference<String> capturedId = new AtomicReference<>();
            org.mockito.Mockito.doAnswer(invocation -> {
                capturedId.set(RequestIdContext.get());
                return null;
            }).when(chain).doFilter(request, response);

            disabledFilter.doFilter(request, response, chain);

            assertThat(capturedId.get()).isNull();
        }
    }

    // ======================================================================
    // ThreadLocal cleanup
    // ======================================================================

    @Nested
    @DisplayName("ThreadLocal 清理")
    class ThreadLocalCleanup {

        @Test
        @DisplayName("filter 完成后应清除 RequestIdContext")
        void cleanupAfterFilter() throws Exception {
            when(request.getHeader("X-Request-ID")).thenReturn("req-123");

            filter.doFilter(request, response, chain);

            assertThat(RequestIdContext.get()).isNull();
        }

        @Test
        @DisplayName("filter chain 异常后也应清除 RequestIdContext")
        void cleanupAfterException() throws Exception {
            when(request.getHeader("X-Request-ID")).thenReturn("req-123");
            org.mockito.Mockito.doThrow(new RuntimeException("chain error"))
                    .when(chain).doFilter(request, response);

            try {
                filter.doFilter(request, response, chain);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("chain error");
            }

            assertThat(RequestIdContext.get()).isNull();
        }
    }

    // ======================================================================
    // Custom header name
    // ======================================================================

    @Nested
    @DisplayName("自定义 header 名称")
    class CustomHeaderName {

        @Test
        @DisplayName("自定义 headerName 时应从指定 header 取值")
        void customHeaderName() throws Exception {
            WebProperties.RequestIdProperties props = new WebProperties.RequestIdProperties();
            props.setHeaderName("X-Custom-Req-ID");
            RequestIdFilter customFilter = new RequestIdFilter(props);

            when(request.getHeader("X-Custom-Req-ID")).thenReturn("custom-req-456");

            AtomicReference<String> capturedId = new AtomicReference<>();
            org.mockito.Mockito.doAnswer(invocation -> {
                capturedId.set(RequestIdContext.get());
                return null;
            }).when(chain).doFilter(request, response);

            customFilter.doFilter(request, response, chain);

            assertThat(capturedId.get()).isEqualTo("custom-req-456");
        }
    }
}
