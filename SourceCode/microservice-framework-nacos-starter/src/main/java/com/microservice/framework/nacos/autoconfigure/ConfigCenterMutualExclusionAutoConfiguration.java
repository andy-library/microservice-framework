package com.microservice.framework.nacos.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Conditional;

/**
 * Enforces the one-config-center rule for framework applications.
 *
 * @author Andy Yang
 */
@AutoConfiguration
@Conditional(NacosAndApolloBothEnabledCondition.class)
public class ConfigCenterMutualExclusionAutoConfiguration {

    public ConfigCenterMutualExclusionAutoConfiguration() {
        throw new ConfigCenterMutualExclusionException(
                "Only one framework config center can be enabled. Disable either framework.nacos.enabled or framework.apollo.enabled.");
    }
}
