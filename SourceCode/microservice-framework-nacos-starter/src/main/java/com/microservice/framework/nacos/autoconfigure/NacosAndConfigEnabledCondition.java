package com.microservice.framework.nacos.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 组合条件：Nacos 启用 AND 配置治理启用
 * <p>
 * 要求 {@code framework.nacos.enabled=true}（默认 true）和
 * {@code framework.config.enabled=true}（默认 true）同时满足。
 * 确保配置治理模块绑定所属配置中心，防止同 classpath 下
 * Nacos 与 Apollo 的 ConfigGovernanceAutoConfiguration 同时激活。
 */
class NacosAndConfigEnabledCondition extends AllNestedConditions {

    NacosAndConfigEnabledCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(prefix = "framework.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class NacosEnabled {}

    @ConditionalOnProperty(prefix = "framework.config", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ConfigEnabled {}
}
