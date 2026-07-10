package com.microservice.framework.json;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.text.DateFormat;

/**
 * JSON Starter 配置属性
 * <p>
 * 所有属性前缀为 {@code framework.json}，控制 JSON 编解码器的
 * 实现选择、安全限制和序列化行为。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.json")
public class JsonProperties {

    /**
     * JSON 实现提供商，默认 jackson
     * <p>
     * 可选值：jackson（默认）、fastjson2。
     * 切换为 fastjson2 需确保 fastjson2 依赖存在于类路径。
     */
    private String provider = "jackson";

    /**
     * 嵌套深度安全限制，默认 2000
     * <p>
     * 超过此深度的 JSON 数据将在反序列化时被拒绝，
     * 防止恶意深度嵌套导致的栈溢出攻击。
     */
    private int maxDepth = 2000;

    /**
     * JSON 数据最大长度限制（字符数），默认 10_000_000（约 10MB）
     * <p>
     * 超过此长度的 JSON 数据将在反序列化时被拒绝，
     * 防止恶意超大 JSON 导致的内存耗尽攻击。
     * 设置为 0 表示不限制长度。
     */
    private int maxPayloadSize = 10_000_000;

    /**
     * 遇到未知属性时是否抛出异常，默认 false
     * <p>
     * 设为 true 时，反序列化过程中遇到 JSON 中存在但目标类中
     * 不存在的属性将抛出异常；设为 false 时将忽略未知属性。
     * 建议生产环境设为 false，调试时设为 true。
     */
    private boolean failOnUnknownProperties = false;

    /**
     * 日期格式，默认 null（使用 ISO-8601）
     * <p>
     * 如果设置了此属性，将覆盖默认的 ISO-8601 日期格式。
     * 仅在 Jackson 实现中生效。
     */
    private DateFormat dateFormat;

    // Getters and Setters

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMaxPayloadSize() {
        return maxPayloadSize;
    }

    public void setMaxPayloadSize(int maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
    }

    public boolean getFailOnUnknownProperties() {
        return failOnUnknownProperties;
    }

    public void setFailOnUnknownProperties(boolean failOnUnknownProperties) {
        this.failOnUnknownProperties = failOnUnknownProperties;
    }

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }
}
