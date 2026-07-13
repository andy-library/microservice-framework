package com.microservice.framework.nacos;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nacos Starter 配置属性
 * <p>
 * 聚合 Nacos 连接、配置源和配置治理等配置项，
 * 所有属性前缀为 {@code framework.nacos}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.nacos")
public class NacosProperties {

    /**
     * 是否启用 Nacos 配置中心集成，默认 true
     */
    private boolean enabled = false;

    /**
     * Nacos 服务器地址，默认 localhost:8848
     */
    private String serverAddr = "localhost:8848";

    /**
     * Nacos 命名空间 ID，默认为空（公共命名空间）
     */
    private String namespace = "";

    /**
     * Nacos 配置分组，默认 DEFAULT_GROUP
     */
    private String group = "DEFAULT_GROUP";

    /**
     * Nacos 配置 Data ID，默认使用 ${spring.application.name}.yml
     */
    private String dataId;

    /**
     * 配置获取超时时间（毫秒），默认 3000
     */
    private long timeout = 3000;

    /**
     * 配置获取最大重试次数，默认 3
     */
    private int maxRetry = 3;

    /**
     * 是否启用 Nacos 服务发现，默认 false
     * <p>
     * V1 版本不自动启用服务发现，需显式配置。
     */
    private boolean serviceDiscoveryEnabled = false;

    /**
     * Kubernetes 配置源
     */
    private KubernetesProperties kubernetes = new KubernetesProperties();

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public boolean isServiceDiscoveryEnabled() {
        return serviceDiscoveryEnabled;
    }

    public void setServiceDiscoveryEnabled(boolean serviceDiscoveryEnabled) {
        this.serviceDiscoveryEnabled = serviceDiscoveryEnabled;
    }

    public KubernetesProperties getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(KubernetesProperties kubernetes) {
        this.kubernetes = kubernetes;
    }

    /**
     * Kubernetes 配置源属性
     */
    public static class KubernetesProperties {

        /**
         * 是否启用 Kubernetes 配置源，默认 false
         * <p>
         * Kubernetes 配置源仅在集群环境中使用，需显式启用。
         */
        private boolean enabled = false;

        /**
         * Kubernetes ConfigMap 名称
         */
        private String configMapName;

        /**
         * Kubernetes Namespace，默认读取环境变量 KUBERNETES_NAMESPACE
         */
        private String namespace;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getConfigMapName() {
            return configMapName;
        }

        public void setConfigMapName(String configMapName) {
            this.configMapName = configMapName;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }
}
