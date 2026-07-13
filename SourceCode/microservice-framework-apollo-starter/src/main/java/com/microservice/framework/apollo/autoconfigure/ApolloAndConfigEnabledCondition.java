package com.microservice.framework.apollo.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 组合条件：Apollo 启用 AND 配置治理启用
 * <p>
 * 要求 {@code framework.apollo.enabled=true}（默认 true）和
 * {@code framework.config.enabled=true}（默认 true）同时满足。
 * 确保配置治理模块绑定所属配置中心，防止同 classpath 下
 * Apollo 与 Nacos 的 ConfigGovernanceAutoConfiguration 同时激活。
 */
class ApolloAndConfigEnabledCondition extends AllNestedConditions {

    ApolloAndConfigEnabledCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(prefix = "framework.apollo", name = "enabled", havingValue = "true")
    static class ApolloEnabled {}

    @ConditionalOnProperty(prefix = "framework.config", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ConfigEnabled {}
}
