package com.microservice.framework.drools;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Drools Starter 配置属性
 * <p>
 * 聚合规则加载、会话管理和运行治理配置组，
 * 所有属性前缀为 {@code framework.drools}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.drools")
public class DroolsProperties {

    /**
     * 规则配置
     */
    @NestedConfigurationProperty
    private RuleProperties rule = new RuleProperties();

    /**
     * 会话配置
     */
    @NestedConfigurationProperty
    private SessionProperties session = new SessionProperties();

    /**
     * 运行治理配置
     */
    @NestedConfigurationProperty
    private GovernanceProperties governance = new GovernanceProperties();

    // Getters and Setters

    public RuleProperties getRule() {
        return rule;
    }

    public void setRule(RuleProperties rule) {
        this.rule = rule;
    }

    public SessionProperties getSession() {
        return session;
    }

    public void setSession(SessionProperties session) {
        this.session = session;
    }

    public GovernanceProperties getGovernance() {
        return governance;
    }

    public void setGovernance(GovernanceProperties governance) {
        this.governance = governance;
    }

    /**
     * 规则配置
     */
    public static class RuleProperties {

        /**
         * 是否启用规则引擎，默认 true
         * <p>
         * 禁用后不会注册 RuleEngine 和 RuleSession Bean，
         * 适用于不需要规则引擎的服务。
         */
        private Boolean enabled = true;

        /**
         * 规则文件路径列表
         * <p>
         * 支持类路径路径（如 "rules/discount.drl"）和文件系统路径。
         * 可配置多个规则文件，按顺序加载。
         */
        @NotNull
        private List<String> ruleFiles = new ArrayList<>();

        /**
         * 规则分组映射
         * <p>
         * 将规则按业务域分组管理，便于按分组查询和执行规则。
         */
        private List<String> groups = new ArrayList<>();

        /**
         * 是否在启动时验证规则文件，默认 true
         * <p>
         * 启用后将在应用启动时验证所有规则文件的语法和逻辑有效性，
         * 防止运行时因规则文件错误导致异常。
         */
        private Boolean validateOnStartup = true;

        // Getters and Setters

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getRuleFiles() {
            return ruleFiles;
        }

        public void setRuleFiles(List<String> ruleFiles) {
            this.ruleFiles = ruleFiles;
        }

        public List<String> getGroups() {
            return groups;
        }

        public void setGroups(List<String> groups) {
            this.groups = groups;
        }

        public Boolean getValidateOnStartup() {
            return validateOnStartup;
        }

        public void setValidateOnStartup(Boolean validateOnStartup) {
            this.validateOnStartup = validateOnStartup;
        }
    }

    /**
     * 会话配置
     */
    public static class SessionProperties {

        /**
         * 最大并发会话数，默认 100
         * <p>
         * 限制同时存在的有状态规则会话数量，防止资源耗尽。
         */
        @Min(1)
        private Integer maxSessions = 100;

        /**
         * 会话超时时间（毫秒），默认 30000
         * <p>
         * 超过此时间未使用的会话将被自动清理释放资源。
         */
        @Min(1000)
        private Long sessionTimeout = 30000L;

        // Getters and Setters

        public Integer getMaxSessions() {
            return maxSessions;
        }

        public void setMaxSessions(Integer maxSessions) {
            this.maxSessions = maxSessions;
        }

        public Long getSessionTimeout() {
            return sessionTimeout;
        }

        public void setSessionTimeout(Long sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
        }
    }

    /**
     * 运行治理配置
     */
    public static class GovernanceProperties {

        /**
         * 是否启用规则执行审计，默认 true
         * <p>
         * 启用后将记录每次规则执行的详细信息，包括触发规则、
         * 输入事实和执行耗时，便于问题排查和合规审计。
         */
        private Boolean auditEnabled = true;

        /**
         * 最大规则执行时间（毫秒），默认 5000
         * <p>
         * 单次规则执行超过此时间将触发告警，
         * 防止规则引擎长时间阻塞业务线程。
         */
        @Min(100)
        private Long maxExecutionTimeMs = 5000L;

        // Getters and Setters

        public Boolean getAuditEnabled() {
            return auditEnabled;
        }

        public void setAuditEnabled(Boolean auditEnabled) {
            this.auditEnabled = auditEnabled;
        }

        public Long getMaxExecutionTimeMs() {
            return maxExecutionTimeMs;
        }

        public void setMaxExecutionTimeMs(Long maxExecutionTimeMs) {
            this.maxExecutionTimeMs = maxExecutionTimeMs;
        }
    }
}
