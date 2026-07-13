package com.microservice.framework.web.autoconfigure;

import com.microservice.framework.web.WebProperties;
import com.microservice.framework.web.api.ApiResponse;
import com.microservice.framework.web.api.ErrorCodeResponse;
import com.microservice.framework.web.context.RequestIdContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Web Starter main auto-configuration.
 * <p>
 * Activates in SERVLET web applications only. Registers the
 * {@link WebProperties} bean and provides default configuration
 * for API response formatting.
 * <p>
 * Condition: {@code framework.web.response.success-code} and related
 * properties are available via {@link WebProperties} binding.
 *
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "framework.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(WebProperties.class)
public class WebAutoConfiguration {

    @Component("responseWrappingAdvice")
    @ControllerAdvice
    @ConditionalOnMissingBean(ResponseBodyAdvice.class)
    static class ResponseWrappingAdvice implements ResponseBodyAdvice<Object> {

        private final WebProperties properties;

        ResponseWrappingAdvice(WebProperties properties) {
            this.properties = properties;
        }

        @Override
        public boolean supports(MethodParameter returnType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
            if (returnType.getDeclaringClass().getPackageName().startsWith("org.springframework.boot.actuate.")) {
                return false;
            }
            Class<?> parameterType = returnType.getParameterType();
            return !ApiResponse.class.isAssignableFrom(parameterType)
                    && !ErrorCodeResponse.class.isAssignableFrom(parameterType)
                    && !StringHttpMessageConverter.class.isAssignableFrom(converterType);
        }

        @Override
        public Object beforeBodyWrite(Object body,
                                      MethodParameter returnType,
                                      MediaType selectedContentType,
                                      Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                      ServerHttpRequest request,
                                      ServerHttpResponse response) {
            if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
                return body;
            }
            if (body instanceof ApiResponse<?> || body instanceof ErrorCodeResponse) {
                return body;
            }
            String requestId = properties.getResponse().isIncludeRequestId()
                    ? RequestIdContext.get()
                    : null;
            Long timestamp = properties.getResponse().isIncludeTimestamp()
                    ? System.currentTimeMillis()
                    : null;
            return ApiResponse.of(
                    properties.getResponse().getSuccessCode(),
                    "success",
                    body,
                    timestamp,
                    requestId);
        }
    }
}
