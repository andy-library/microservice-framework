package com.microservice.framework.nacos;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置治理属性
 * <p>
 * 聚合配置校验、刷新策略和敏感值脱敏等配置项，
 * 所有属性前缀为 {@code framework.config}。
 * <p>
 * 配置治理模块与配置中心（Nacos/Apollo/Kubernetes）解耦，
 * 可独立于具体配置中心使用。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.config")
public class ConfigGovernanceProperties {

    /**
     * 是否启用配置治理，默认 true
     */
    private boolean enabled = true;

    /**
     * 配置校验属性
     */
    private ValidatorProperties validator = new ValidatorProperties();

    /**
     * 刷新策略属性
     */
    private RefreshPolicyProperties refreshPolicy = new RefreshPolicyProperties();

    /**
     * 敏感值脱敏属性
     */
    private MaskingProperties masking = new MaskingProperties();

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ValidatorProperties getValidator() {
        return validator;
    }

    public void setValidator(ValidatorProperties validator) {
        this.validator = validator;
    }

    public RefreshPolicyProperties getRefreshPolicy() {
        return refreshPolicy;
    }

    public void setRefreshPolicy(RefreshPolicyProperties refreshPolicy) {
        this.refreshPolicy = refreshPolicy;
    }

    public MaskingProperties getMasking() {
        return masking;
    }

    public void setMasking(MaskingProperties masking) {
        this.masking = masking;
    }

    /**
     * 配置校验属性
     */
    public static class ValidatorProperties {

        /**
         * 是否启用配置校验，默认 true
         */
        private boolean enabled = true;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 刷新策略属性
     */
    public static class RefreshPolicyProperties {

        /**
         * 是否启用刷新策略控制，默认 true
         */
        private boolean enabled = true;

        /**
         * 默认刷新策略，可选值：ON_CHANGE、MANUAL、NONE，默认 ON_CHANGE
         */
        private String defaultStrategy = "ON_CHANGE";

        /**
         * 不可刷新的配置键前缀列表
         * <p>
         * 匹配前缀的配置键在配置变更时不会触发动态刷新。
         */
        private List<String> nonRefreshablePrefixes = new ArrayList<>();

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDefaultStrategy() {
            return defaultStrategy;
        }

        public void setDefaultStrategy(String defaultStrategy) {
            this.defaultStrategy = defaultStrategy;
        }

        public List<String> getNonRefreshablePrefixes() {
            return nonRefreshablePrefixes;
        }

        public void setNonRefreshablePrefixes(List<String> nonRefreshablePrefixes) {
            this.nonRefreshablePrefixes = nonRefreshablePrefixes;
        }
    }

    /**
     * 敏感值脱敏属性
     */
    public static class MaskingProperties {

        /**
         * 是否启用敏感值脱敏，默认 true
         */
        private boolean enabled = true;

        /**
         * 敏感配置键匹配模式列表
         * <p>
         * 默认匹配包含 password、secret、key、token、credential、pwd 的配置键。
         */
        private List<String> sensitiveKeyPatterns = new ArrayList<>(List.of(
                "password", "secret", "key", "token", "credential", "pwd"
        ));

        /**
         * 脱敏显示的掩码字符串，默认 "***"
         */
        private String maskValue = "***";

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getSensitiveKeyPatterns() {
            return sensitiveKeyPatterns;
        }

        public void setSensitiveKeyPatterns(List<String> sensitiveKeyPatterns) {
            this.sensitiveKeyPatterns = sensitiveKeyPatterns;
        }

        public String getMaskValue() {
            return maskValue;
        }

        public void setMaskValue(String maskValue) {
            this.maskValue = maskValue;
        }
    }
}
