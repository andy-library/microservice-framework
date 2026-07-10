package com.microservice.framework.nacos.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Matches when both Nacos and Apollo framework switches are enabled.
 *
 * @author Andy Yang
 */
class NacosAndApolloBothEnabledCondition extends AllNestedConditions {

    NacosAndApolloBothEnabledCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(prefix = "framework.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class NacosEnabled {
    }

    @ConditionalOnProperty(prefix = "framework.apollo", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ApolloEnabled {
    }
}
