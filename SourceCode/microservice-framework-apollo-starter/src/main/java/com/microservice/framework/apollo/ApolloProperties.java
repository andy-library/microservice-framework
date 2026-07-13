package com.microservice.framework.apollo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Apollo Starter 配置属性
 * <p>
 * 聚合 Apollo 连接、配置源和配置治理等配置项，
 * 所有属性前缀为 {@code framework.apollo}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.apollo")
public class ApolloProperties {

    /**
     * 是否启用 Apollo 配置中心集成，默认 true
     */
    private boolean enabled = false;

    /**
     * Apollo AppId，应用唯一标识
     * <p>
     * 当 Apollo 客户端存在于类路径时，AppId 不能为空。
     */
    private String appId;

    /**
     * Apollo 集群名称，默认 "default"
     */
    private String cluster = "default";

    /**
     * Apollo 命名空间列表，默认 ["application"]
     * <p>
     * 仅允许列出的命名空间加载配置，未列出的命名空间将被忽略。
     */
    private List<String> namespaces = new ArrayList<>(List.of("application"));

    /**
     * Apollo 环境（DEV/FAT/UAT/PRO），可选配置
     */
    private String env;

    /**
     * Apollo Meta Server 地址
     * <p>
     * Apollo 客户端通过 Meta Server 发现 Config Service。
     */
    private String metaServerUrl;

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

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<String> namespaces) {
        this.namespaces = namespaces;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getMetaServerUrl() {
        return metaServerUrl;
    }

    public void setMetaServerUrl(String metaServerUrl) {
        this.metaServerUrl = metaServerUrl;
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
