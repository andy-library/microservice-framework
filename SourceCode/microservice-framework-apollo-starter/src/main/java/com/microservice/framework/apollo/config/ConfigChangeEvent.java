package com.microservice.framework.apollo.config;

import java.util.Objects;

/**
 * 配置变更事件
 * <p>
 * 当配置中心的配置发生变更时发布此事件，携带变更的配置键、旧值、新值
 * 以及配置源描述信息。应用可通过监听此事件实现配置变更的响应逻辑。
 * <p>
 * 不可变对象，通过 Builder 或构造方法创建。
 *
 * @author Andy Yang
 */
public final class ConfigChangeEvent {

    private final String key;
    private final String oldValue;
    private final String newValue;
    private final ConfigSourceDescriptor source;
    private final long changeTimestamp;

    /**
     * 创建配置变更事件
     *
     * @param key             变更的配置键
     * @param oldValue        变更前的值（可为 null 表示新增配置）
     * @param newValue        变更后的值（可为 null 表示删除配置）
     * @param source          配置源描述符
     * @param changeTimestamp 变更发生的时间戳
     */
    public ConfigChangeEvent(String key, String oldValue, String newValue,
                             ConfigSourceDescriptor source, long changeTimestamp) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.changeTimestamp = changeTimestamp;
    }

    /**
     * 判断此事件是否为新增配置
     *
     * @return 如果旧值为 null，返回 true
     */
    public boolean isAddition() {
        return oldValue == null && newValue != null;
    }

    /**
     * 判断此事件是否为删除配置
     *
     * @return 如果新值为 null，返回 true
     */
    public boolean isDeletion() {
        return newValue == null && oldValue != null;
    }

    /**
     * 判断此事件是否为修改配置
     *
     * @return 如果旧值和新值均不为 null，返回 true
     */
    public boolean isModification() {
        return oldValue != null && newValue != null;
    }

    // Getters

    public String getKey() {
        return key;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public ConfigSourceDescriptor getSource() {
        return source;
    }

    public long getChangeTimestamp() {
        return changeTimestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConfigChangeEvent)) {
            return false;
        }
        ConfigChangeEvent other = (ConfigChangeEvent) obj;
        return Objects.equals(key, other.key)
                && Objects.equals(oldValue, other.oldValue)
                && Objects.equals(newValue, other.newValue)
                && Objects.equals(source, other.source)
                && changeTimestamp == other.changeTimestamp;
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + Objects.hashCode(oldValue);
        result = 31 * result + Objects.hashCode(newValue);
        result = 31 * result + source.hashCode();
        result = 31 * result + Long.hashCode(changeTimestamp);
        return result;
    }

    @Override
    public String toString() {
        return "ConfigChangeEvent{" +
                "key='" + key + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                ", source=" + source +
                ", changeTimestamp=" + changeTimestamp +
                '}';
    }
}
