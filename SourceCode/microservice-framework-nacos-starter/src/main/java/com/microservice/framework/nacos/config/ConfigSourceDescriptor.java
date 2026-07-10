package com.microservice.framework.nacos.config;

import java.util.Objects;

/**
 * 配置源描述符
 * <p>
 * 描述配置的来源信息，包括配置中心类型、Data ID、分组、命名空间
 * 和获取时间戳。用于标识配置的来源，支持 Nacos、Kubernetes、
 * 环境变量等多种配置源。
 * <p>
 * 不可变对象，通过 Builder 或静态工厂方法创建。
 *
 * @author Andy Yang
 */
public final class ConfigSourceDescriptor {

    /**
     * 配置源类型枚举
     */
    public enum SourceType {
        /** Nacos 配置中心 */
        NACOS,
        /** Kubernetes ConfigMap */
        KUBERNETES,
        /** 系统环境变量 */
        ENVIRONMENT,
        /** Spring Boot 配置文件 */
        SPRING_BOOT,
        /** 自定义配置源 */
        CUSTOM
    }

    private final SourceType sourceType;
    private final String dataId;
    private final String group;
    private final String namespace;
    private final long timestamp;

    private ConfigSourceDescriptor(SourceType sourceType, String dataId,
                                   String group, String namespace, long timestamp) {
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.dataId = dataId;
        this.group = group;
        this.namespace = namespace;
        this.timestamp = timestamp;
    }

    /**
     * 创建 Nacos 配置源描述符
     *
     * @param dataId    配置 Data ID
     * @param group     配置分组
     * @param namespace 命名空间
     * @param timestamp 获取时间戳
     * @return Nacos 配置源描述符
     */
    public static ConfigSourceDescriptor nacos(String dataId, String group,
                                                String namespace, long timestamp) {
        return new ConfigSourceDescriptor(SourceType.NACOS, dataId, group, namespace, timestamp);
    }

    /**
     * 创建 Kubernetes ConfigMap 配置源描述符
     *
     * @param configMapName ConfigMap 名称
     * @param namespace     Kubernetes Namespace
     * @param timestamp     获取时间戳
     * @return Kubernetes 配置源描述符
     */
    public static ConfigSourceDescriptor kubernetes(String configMapName,
                                                     String namespace, long timestamp) {
        return new ConfigSourceDescriptor(SourceType.KUBERNETES, configMapName,
                namespace, namespace, timestamp);
    }

    /**
     * 创建环境变量配置源描述符
     *
     * @param timestamp 获取时间戳
     * @return 环境变量配置源描述符
     */
    public static ConfigSourceDescriptor environment(long timestamp) {
        return new ConfigSourceDescriptor(SourceType.ENVIRONMENT, null,
                null, null, timestamp);
    }

    /**
     * 创建 Spring Boot 配置文件源描述符
     *
     * @param dataId    配置文件名称
     * @param timestamp 获取时间戳
     * @return Spring Boot 配置源描述符
     */
    public static ConfigSourceDescriptor springBoot(String dataId, long timestamp) {
        return new ConfigSourceDescriptor(SourceType.SPRING_BOOT, dataId,
                null, null, timestamp);
    }

    /**
     * 创建自定义配置源描述符
     *
     * @param dataId    配置标识
     * @param timestamp 获取时间戳
     * @return 自定义配置源描述符
     */
    public static ConfigSourceDescriptor custom(String dataId, long timestamp) {
        return new ConfigSourceDescriptor(SourceType.CUSTOM, dataId,
                null, null, timestamp);
    }

    // Getters

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getDataId() {
        return dataId;
    }

    public String getGroup() {
        return group;
    }

    public String getNamespace() {
        return namespace;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConfigSourceDescriptor)) {
            return false;
        }
        ConfigSourceDescriptor other = (ConfigSourceDescriptor) obj;
        return sourceType == other.sourceType
                && Objects.equals(dataId, other.dataId)
                && Objects.equals(group, other.group)
                && Objects.equals(namespace, other.namespace)
                && timestamp == other.timestamp;
    }

    @Override
    public int hashCode() {
        int result = sourceType.hashCode();
        result = 31 * result + Objects.hashCode(dataId);
        result = 31 * result + Objects.hashCode(group);
        result = 31 * result + Objects.hashCode(namespace);
        result = 31 * result + Long.hashCode(timestamp);
        return result;
    }

    @Override
    public String toString() {
        return "ConfigSourceDescriptor{" +
                "sourceType=" + sourceType +
                ", dataId='" + dataId + '\'' +
                ", group='" + group + '\'' +
                ", namespace='" + namespace + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
