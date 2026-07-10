package com.microservice.framework.nacos.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 敏感配置值脱敏器
 * <p>
 * 对包含敏感信息（如密码、密钥、令牌）的配置值进行脱敏处理，
 * 确保日志输出和监控展示中不暴露明文。
 * <p>
 * 通过配置键的模式匹配判断是否为敏感键，匹配到的值将替换为掩码字符串。
 * 默认敏感键模式包括：password、secret、key、token、credential、pwd。
 * <p>
 * 不可变对象，通过构造方法创建。
 *
 * @author Andy Yang
 */
public final class SensitiveConfigMasker {

    private final Set<String> sensitiveKeyPatterns;
    private final String maskValue;

    /**
     * 创建脱敏器
     *
     * @param sensitiveKeyPatterns 敏感配置键匹配模式集合（大小写不敏感）
     * @param maskValue            脱敏显示的掩码字符串
     */
    public SensitiveConfigMasker(Set<String> sensitiveKeyPatterns, String maskValue) {
        this.sensitiveKeyPatterns = sensitiveKeyPatterns == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(lowercaseAll(sensitiveKeyPatterns)));
        this.maskValue = Objects.requireNonNull(maskValue, "maskValue must not be null");
    }

    /**
     * 创建默认脱敏器
     * <p>
     * 默认敏感键模式：password、secret、key、token、credential、pwd
     * 默认掩码值：{@code "***"}
     *
     * @return 默认脱敏器
     */
    public static SensitiveConfigMasker defaultMasker() {
        return new SensitiveConfigMasker(
                Set.of("password", "secret", "key", "token", "credential", "pwd"),
                "***"
        );
    }

    /**
     * 判断指定配置键是否为敏感键
     * <p>
     * 匹配逻辑：配置键（转换为小写）是否包含任何敏感模式字符串。
     * 例如：键 "database.password" 包含模式 "password"，因此为敏感键。
     *
     * @param key 配置键
     * @return 是否为敏感键
     */
    public boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String lowerKey = key.toLowerCase();
        for (String pattern : sensitiveKeyPatterns) {
            if (lowerKey.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对指定配置值进行脱敏处理
     * <p>
     * 如果配置键为敏感键，返回掩码字符串；否则返回原始值。
     *
     * @param key   配置键
     * @param value 配置值
     * @return 脱敏后的值或原始值
     */
    public String mask(String key, String value) {
        if (isSensitive(key)) {
            return maskValue;
        }
        return value;
    }

    /**
     * 对一组配置键值对进行批量脱敏处理
     * <p>
     * 返回新的键值对列表，其中敏感值已替换为掩码字符串。
     * 原始列表不会被修改。
     *
     * @param entries 配置键值对列表
     * @return 脱敏后的键值对列表
     */
    public List<ConfigEntry> maskAll(List<ConfigEntry> entries) {
        if (entries == null) {
            return Collections.emptyList();
        }
        List<ConfigEntry> result = new ArrayList<>(entries.size());
        for (ConfigEntry entry : entries) {
            result.add(new ConfigEntry(entry.getKey(), mask(entry.getKey(), entry.getValue())));
        }
        return result;
    }

    // Getters

    public Set<String> getSensitiveKeyPatterns() {
        return sensitiveKeyPatterns;
    }

    public String getMaskValue() {
        return maskValue;
    }

    private static List<String> lowercaseAll(Set<String> patterns) {
        List<String> result = new ArrayList<>(patterns.size());
        for (String pattern : patterns) {
            result.add(pattern.toLowerCase());
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SensitiveConfigMasker)) {
            return false;
        }
        SensitiveConfigMasker other = (SensitiveConfigMasker) obj;
        return sensitiveKeyPatterns.equals(other.sensitiveKeyPatterns)
                && maskValue.equals(other.maskValue);
    }

    @Override
    public int hashCode() {
        int result = sensitiveKeyPatterns.hashCode();
        result = 31 * result + maskValue.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SensitiveConfigMasker{" +
                "sensitiveKeyPatterns=" + sensitiveKeyPatterns +
                ", maskValue='" + maskValue + '\'' +
                '}';
    }

    /**
     * 配置键值对，用于批量脱敏操作的输入和输出
     */
    public static final class ConfigEntry {

        private final String key;
        private final String value;

        public ConfigEntry(String key, String value) {
            this.key = Objects.requireNonNull(key, "key must not be null");
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ConfigEntry)) {
                return false;
            }
            ConfigEntry other = (ConfigEntry) obj;
            return key.equals(other.key) && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            int result = key.hashCode();
            result = 31 * result + Objects.hashCode(value);
            return result;
        }

        @Override
        public String toString() {
            return "ConfigEntry{key='" + key + "', value='" + value + "'}";
        }
    }
}
