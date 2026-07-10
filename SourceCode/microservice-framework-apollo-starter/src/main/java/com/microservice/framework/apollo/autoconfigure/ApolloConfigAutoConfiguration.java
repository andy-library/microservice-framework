package com.microservice.framework.apollo.autoconfigure;

import com.microservice.framework.apollo.ApolloProperties;
import com.microservice.framework.apollo.ConfigGovernanceProperties;
import com.microservice.framework.apollo.config.ConfigSourceDescriptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Apollo 配置中心自动配置
 * <p>
 * 根据 {@code framework.apollo.enabled} 属性决定是否激活 Apollo 配置中心集成，
 * 默认启用。当 Apollo 客户端类在类路径上时，注册 Apollo 配置源描述符。
 * <p>
 * 包含 Nacos Starter 互斥检测：当 Nacos 类同时存在于类路径时，
 * 抛出 {@link NacosStarterConflictException} 阻止应用启动。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties({ApolloProperties.class, ConfigGovernanceProperties.class})
@ConditionalOnProperty(prefix = "framework.apollo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApolloConfigAutoConfiguration {

    /**
     * Nacos Starter 互斥检测
     * <p>
     * 当 Nacos 的配置服务类存在于类路径时，立即抛出异常阻止应用启动。
     * Apollo 和 Nacos 不能同时作为配置中心使用。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.alibaba.nacos.client.config.NacosConfigService")
    static class NacosConflictConfiguration {

        @Bean
        NacosConflictDetector nacosConflictDetector() {
            throw new NacosStarterConflictException();
        }
    }

    /**
     * Apollo 配置源描述符注册
     * <p>
     * 当 Apollo 客户端 API 类存在于类路径时，注册默认的 Apollo 配置源描述符。
     * 同时校验 appId 不为空。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.ctrip.framework.apollo.spring.boot.ApolloAutoConfiguration")
    static class ApolloConfigBindingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ConfigSourceDescriptor apolloConfigSourceDescriptor(ApolloProperties properties) {
            if (properties.getAppId() == null || properties.getAppId().isBlank()) {
                throw new IllegalArgumentException(
                        "Apollo appId must not be blank when Apollo client is on the classpath. " +
                        "Please set 'framework.apollo.app-id' in your configuration.");
            }
            return ConfigSourceDescriptor.apollo(
                    properties.getAppId(),
                    properties.getCluster(),
                    properties.getNamespaces(),
                    System.currentTimeMillis()
            );
        }
    }

    /**
     * Nacos 互斥冲突检测标记类
     * <p>
     * 此 Bean 永远不会被成功创建——当 Nacos 存在于类路径时，
     * 其创建过程会抛出 {@link NacosStarterConflictException}。
     */
    static class NacosConflictDetector {
        // Marker class - bean creation always throws NacosStarterConflictException
    }
}
