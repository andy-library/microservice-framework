package com.microservice.framework.nacos.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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

    @ConditionalOnProperty(prefix = "framework.nacos", name = "enabled", havingValue = "true")
    static class NacosEnabled {
    }

    @ConditionalOnClass(name = "com.microservice.framework.apollo.ApolloProperties")
    static class ApolloStarterPresent {
    }

    @ConditionalOnProperty(prefix = "framework.apollo", name = "enabled", havingValue = "true")
    static class ApolloEnabled {
    }
}
