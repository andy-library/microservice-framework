package com.microservice.framework.web.autoconfigure;

import com.microservice.framework.web.WebProperties;
import com.microservice.framework.web.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Exception handler auto-configuration.
 * <p>
 * Registers {@link GlobalExceptionHandler} as a Spring bean when
 * the web application is SERVLET-based and exception handling is enabled.
 * <p>
 * Condition: {@code framework.web.exception.enabled=true} (defaults to true).
 * Users can provide their own exception handler bean to override the default.
 *
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "framework.web.exception", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(WebProperties.class)
public class ExceptionHandlerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(WebProperties properties) {
        return new GlobalExceptionHandler(properties);
    }
}
