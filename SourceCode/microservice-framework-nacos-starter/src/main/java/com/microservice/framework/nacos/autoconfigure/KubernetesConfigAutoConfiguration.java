package com.microservice.framework.nacos.autoconfigure;

import com.microservice.framework.nacos.NacosProperties;
import com.microservice.framework.nacos.config.ConfigSourceDescriptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kubernetes 配置源自动配置
 * <p>
 * 根据 {@code framework.nacos.kubernetes.enabled} 属性决定是否激活
 * Kubernetes ConfigMap 配置源，默认不启用。
 * <p>
 * Kubernetes 配置源作为 Nacos 的补充，在云原生环境中从 ConfigMap
 * 读取配置。需显式启用，且仅在 Kubernetes 环境中使用。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(NacosProperties.class)
@ConditionalOnProperty(prefix = "framework.nacos.kubernetes", name = "enabled", havingValue = "true")
public class KubernetesConfigAutoConfiguration {

    /**
     * 注册 Kubernetes 配置源描述符
     * <p>
     * 默认从 {@link NacosProperties} 中获取 ConfigMap 名称和 Namespace。
     * 用户可通过注册自定义 {@link ConfigSourceDescriptor} Bean 覆盖。
     */
    @Configuration(proxyBeanMethods = false)
    static class KubernetesConfigSourceConfiguration {

        @Bean("kubernetesConfigSourceDescriptor")
        @ConditionalOnMissingBean(name = "kubernetesConfigSourceDescriptor")
        ConfigSourceDescriptor kubernetesConfigSourceDescriptor(NacosProperties properties) {
            NacosProperties.KubernetesProperties k8s = properties.getKubernetes();
            return ConfigSourceDescriptor.kubernetes(
                    k8s.getConfigMapName(),
                    k8s.getNamespace(),
                    System.currentTimeMillis()
            );
        }
    }
}
