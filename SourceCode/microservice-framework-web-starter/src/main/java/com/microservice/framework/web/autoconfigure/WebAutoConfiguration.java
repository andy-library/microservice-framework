package com.microservice.framework.web.autoconfigure;

import com.microservice.framework.web.WebProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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

}
