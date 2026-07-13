package com.microservice.framework.apollo.autoconfigure;

import com.microservice.framework.apollo.ConfigGovernanceProperties;
import com.microservice.framework.apollo.config.ConfigValidator;
import com.microservice.framework.apollo.config.RefreshPolicy;
import com.microservice.framework.apollo.config.SensitiveConfigMasker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * 配置治理自动配置
 * <p>
 * 根据 {@code framework.config.enabled} 属性决定是否激活配置治理功能，
 * 默认启用。配置治理模块提供配置校验、刷新策略控制和敏感值脱敏，
 * 与具体配置中心（Apollo/Nacos/Kubernetes）解耦，可独立使用。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(ConfigGovernanceProperties.class)
@Conditional(ApolloAndConfigEnabledCondition.class)
public class ConfigGovernanceAutoConfiguration {

    /**
     * 配置校验器注册
     * <p>
     * 当 {@code framework.config.validator.enabled} 为 true（默认）时，
     * 注册默认的必填校验器。用户可通过注册自定义 {@link ConfigValidator} Bean 覆盖。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "framework.config.validator", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ValidatorConfiguration {

        @Bean(name = "apolloConfigValidator")
        @ConditionalOnMissingBean
        ConfigValidator defaultConfigValidator() {
            return ConfigValidator.required();
        }
    }

    /**
     * 刷新策略注册
     * <p>
     * 当 {@code framework.config.refresh-policy.enabled} 为 true（默认）时，
     * 注册默认的刷新策略。用户可通过注册自定义 {@link RefreshPolicy} Bean 覆盖。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "framework.config.refresh-policy", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class RefreshPolicyConfiguration {

        @Bean(name = "apolloRefreshPolicy")
        @ConditionalOnMissingBean
        RefreshPolicy defaultRefreshPolicy(ConfigGovernanceProperties properties) {
            ConfigGovernanceProperties.RefreshPolicyProperties rp = properties.getRefreshPolicy();
            RefreshPolicy.Strategy strategy = RefreshPolicy.Strategy.valueOf(rp.getDefaultStrategy());
            Set<String> nonRefreshablePrefixes = new HashSet<>(rp.getNonRefreshablePrefixes());
            return new RefreshPolicy(strategy, nonRefreshablePrefixes);
        }
    }

    /**
     * 敏感值脱敏器注册
     * <p>
     * 当 {@code framework.config.masking.enabled} 为 true（默认）时，
     * 注册默认的敏感配置脱敏器。用户可通过注册自定义 {@link SensitiveConfigMasker} Bean 覆盖。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "framework.config.masking", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class MaskingConfiguration {

        @Bean(name = "apolloSensitiveConfigMasker")
        @ConditionalOnMissingBean
        SensitiveConfigMasker defaultSensitiveConfigMasker(ConfigGovernanceProperties properties) {
            ConfigGovernanceProperties.MaskingProperties mp = properties.getMasking();
            return new SensitiveConfigMasker(
                    new HashSet<>(mp.getSensitiveKeyPatterns()),
                    mp.getMaskValue()
            );
        }
    }
}
