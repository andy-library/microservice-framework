package com.microservice.framework.web.autoconfigure;

import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import com.microservice.framework.web.WebProperties;
import com.microservice.framework.web.context.RequestIdFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Request ID auto-configuration.
 * <p>
 * Registers {@link RequestIdFilter} as a Spring bean when the web
 * application is SERVLET-based and request ID propagation is enabled.
 * <p>
 * Condition: {@code framework.web.request-id.enabled=true} (defaults to true).
 * Users can provide their own request ID filter bean to override the default.
 *
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "framework.web.request-id", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(WebProperties.class)
public class RequestIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestIdFilter requestIdFilter(
            WebProperties properties,
            ObjectProvider<ThreadLocalContextAdapter> contextAdapter) {
        return new RequestIdFilter(
                properties.getRequestId(),
                contextAdapter.getIfAvailable(ThreadLocalContextAdapter::new));
    }
}
