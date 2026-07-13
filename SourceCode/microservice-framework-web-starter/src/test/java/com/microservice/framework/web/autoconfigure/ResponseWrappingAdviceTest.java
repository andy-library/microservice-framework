package com.microservice.framework.web.autoconfigure;

import com.microservice.framework.web.WebProperties;
import com.microservice.framework.web.api.ApiResponse;
import com.microservice.framework.web.context.RequestIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
class ResponseWrappingAdviceTest {

    private final WebProperties properties = new WebProperties();
    private final WebAutoConfiguration.ResponseWrappingAdvice advice =
            new WebAutoConfiguration.ResponseWrappingAdvice(properties);

    @AfterEach
    void tearDown() {
        RequestIdContext.remove();
    }

    @Test
    @DisplayName("普通 Controller 返回值必须包装为 ApiResponse")
    void plainBodyShouldBeWrapped() throws Exception {
        RequestIdContext.set("req-123");

        Object body = advice.beforeBodyWrite(
                "payload",
                returnType("plainString"),
                MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class,
                null,
                null);

        assertThat(body).isInstanceOf(ApiResponse.class);
        ApiResponse<?> response = (ApiResponse<?>) body;
        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData()).isEqualTo("payload");
        assertThat(response.getRequestId()).isEqualTo("req-123");
    }

    @Test
    @DisplayName("已包装响应必须作为受控例外保持原样")
    void apiResponseShouldNotBeWrappedAgain() throws Exception {
        ApiResponse<String> original = ApiResponse.success("payload");

        Object body = advice.beforeBodyWrite(
                original,
                returnType("apiResponse"),
                MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class,
                null,
                null);

        assertThat(body).isSameAs(original);
    }

    @Test
    @DisplayName("StringHttpMessageConverter 作为受控例外不包装，避免类型不匹配")
    void stringConverterShouldBeControlledExemption() throws Exception {
        Object body = advice.beforeBodyWrite(
                "payload",
                returnType("plainString"),
                MediaType.TEXT_PLAIN,
                StringHttpMessageConverter.class,
                null,
                null);

        assertThat(body).isEqualTo("payload");
    }

    private static MethodParameter returnType(String methodName) throws NoSuchMethodException {
        Method method = SampleController.class.getDeclaredMethod(methodName);
        return new MethodParameter(method, -1);
    }

    static class SampleController {
        String plainString() {
            return "";
        }

        ApiResponse<String> apiResponse() {
            return ApiResponse.success("");
        }
    }
}
