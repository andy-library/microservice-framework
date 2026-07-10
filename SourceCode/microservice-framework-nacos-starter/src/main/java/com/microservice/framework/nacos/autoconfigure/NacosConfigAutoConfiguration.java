package com.microservice.framework.nacos.autoconfigure;

import com.microservice.framework.nacos.ConfigGovernanceProperties;
import com.microservice.framework.nacos.NacosProperties;
import com.microservice.framework.nacos.config.ConfigSourceDescriptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 配置中心自动配置
 * <p>
 * 根据 {@code framework.nacos.enabled} 属性决定是否激活 Nacos 配置中心集成，
 * 默认启用。当 Nacos 客户端类在类路径上时，注册 Nacos 配置源描述符。
 * <p>
 * 包含 Apollo Starter 互斥检测：当 Apollo 类同时存在于类路径时，
 * 抛出 {@link ApolloStarterConflictException} 阻止应用启动。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties({NacosProperties.class, ConfigGovernanceProperties.class})
@ConditionalOnProperty(prefix = "framework.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NacosConfigAutoConfiguration {

    /**
     * Apollo Starter 互斥检测
     * <p>
     * 当 Apollo 的自动配置类存在于类路径时，立即抛出异常阻止应用启动。
     * Nacos 和 Apollo 不能同时作为配置中心使用。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.ctrip.framework.apollo.spring.boot.ApolloAutoConfiguration")
    static class ApolloConflictConfiguration {

        @Bean
        ApolloConflictDetector apolloConflictDetector() {
            throw new ApolloStarterConflictException();
        }
    }

    /**
     * Nacos 配置源描述符注册
     * <p>
     * 当 Nacos 客户端 API 类存在于类路径时，注册默认的 Nacos 配置源描述符。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.alibaba.nacos.api.config.ConfigService")
    static class NacosConfigBindingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ConfigSourceDescriptor nacosConfigSourceDescriptor(NacosProperties properties) {
            return ConfigSourceDescriptor.nacos(
                    properties.getDataId(),
                    properties.getGroup(),
                    properties.getNamespace(),
                    System.currentTimeMillis()
            );
        }
    }

    /**
     * Apollo 互斥冲突检测标记类
     * <p>
     * 此 Bean 永远不会被成功创建——当 Apollo 存在于类路径时，
     * 其创建过程会抛出 {@link ApolloStarterConflictException}。
     */
    static class ApolloConflictDetector {
        // Marker class - bean creation always throws ApolloStarterConflictException
    }
}
