package com.microservice.framework.drools.autoconfigure;

import com.microservice.framework.drools.DroolsProperties;
import com.microservice.framework.drools.api.RuleEngine;
import com.microservice.framework.drools.api.RuleExecutionResult;
import com.microservice.framework.drools.api.RuleSession;
import com.microservice.framework.drools.api.RuleVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Drools Starter 自动配置
 * <p>
 * 根据 {@code framework.drools.rule.enabled} 属性决定是否激活，默认启用。
 * 注册以下 Bean：
 * - {@link RuleEngine}：规则引擎核心接口
 * - {@link RuleVersion}：当前规则包版本信息
 * <p>
 * 当 {@code framework.drools.session.enabled=true}（默认）时，
 * 还注册 {@link RuleSession} 的创建能力。
 * <p>
 * 当 Drools 运行时（drools-core）不存在时，注册基于内存的默认实现，
 * 仅提供接口骨架，不执行实际规则逻辑。

 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(DroolsProperties.class)
@ConditionalOnProperty(prefix = "framework.drools.rule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DroolsAutoConfiguration {

    /**
     * 注册 RuleEngine Bean
     * <p>
     * 当容器中不存在 RuleEngine 时，注册基于内存的默认实现。
     * 默认实现仅提供接口骨架，实际规则执行需要 Drools 运行时依赖。
     *
     * @param properties Drools 配置属性
     * @return RuleEngine 实例
     */
    @Bean
    @ConditionalOnMissingBean(RuleEngine.class)
    public RuleEngine ruleEngine(DroolsProperties properties) {
        return new DefaultRuleEngine(properties);
    }

    /**
     * 注册 RuleVersion Bean
     * <p>
     * 提供当前规则包的版本信息，用于运行治理和版本追踪。
     *
     * @param properties Drools 配置属性
     * @return RuleVersion 实例
     */
    @Bean
    @ConditionalOnMissingBean(RuleVersion.class)
    public RuleVersion ruleVersion(DroolsProperties properties) {
        return new RuleVersion(
                "com.microservice.framework",
                "drools-rules",
                "1.0.0-SNAPSHOT",
                properties.getRule().getRuleFiles().size());
    }

    // ========================================================================
    // 默认内部实现
    // ========================================================================

    /**
     * 基于 API 的默认 RuleEngine 实现
     * <p>
     * 当 Drools 运行时依赖不存在时的兜底实现，
     * 仅提供接口骨架，所有规则执行返回空结果。
     */
    static class DefaultRuleEngine implements RuleEngine {

        private final DroolsProperties properties;

        DefaultRuleEngine(DroolsProperties properties) {
            this.properties = properties;
        }

        @Override
        public RuleExecutionResult execute(Collection<?> facts) {
            return RuleExecutionResult.empty(0);
        }

        @Override
        public RuleExecutionResult executeWithFacts(Collection<?> facts, Map<String, Object> globals) {
            return RuleExecutionResult.empty(0);
        }

        @Override
        public boolean validate() {
            return true;
        }

        @Override
        public int getRuleCount() {
            return properties.getRule().getRuleFiles().size();
        }

        @Override
        public List<String> getRuleGroup(String group) {
            return Collections.emptyList();
        }
    }
}
