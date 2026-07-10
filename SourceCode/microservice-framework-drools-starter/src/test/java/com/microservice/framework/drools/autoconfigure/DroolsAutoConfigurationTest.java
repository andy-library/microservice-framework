package com.microservice.framework.drools.autoconfigure;

import com.microservice.framework.drools.DroolsProperties;
import com.microservice.framework.drools.api.RuleEngine;
import com.microservice.framework.drools.api.RuleVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drools Starter 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证自动配置的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 *
 * @author Andy Yang
 */
class DroolsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DroolsAutoConfiguration.class));

    @Test
    @DisplayName("默认配置应激活 Drools 自动配置")
    void defaultConfigurationShouldActivateDrools() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("ruleEngine");
            assertThat(context).hasBean("ruleVersion");
        });
    }

    @Test
    @DisplayName("禁用 Drools Starter 后所有 Bean 不应存在")
    void disablingDroolsShouldRemoveAllBeans() {
        contextRunner.withPropertyValues("framework.drools.rule.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(RuleEngine.class);
                    assertThat(context).doesNotHaveBean(RuleVersion.class);
                });
    }

    @Test
    @DisplayName("RuleEngine 应具备基本方法")
    void ruleEngineShouldHaveBasicMethods() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            assertThat(engine.validate()).isTrue();
            assertThat(engine.getRuleCount()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("RuleVersion 应包含默认版本信息")
    void ruleVersionShouldContainDefaultVersionInfo() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RuleVersion version = context.getBean(RuleVersion.class);
            assertThat(version.getGroupId()).isEqualTo("com.microservice.framework");
            assertThat(version.getArtifactId()).isEqualTo("drools-rules");
            assertThat(version.getVersion()).isEqualTo("1.0.0-SNAPSHOT");
            assertThat(version.getCoordinates()).isEqualTo("com.microservice.framework:drools-rules:1.0.0-SNAPSHOT");
        });
    }

    @Test
    @DisplayName("配置规则文件后 RuleEngine 应返回规则数量")
    void configuredRuleFilesShouldReflectInRuleCount() {
        contextRunner.withPropertyValues(
                "framework.drools.rule.rule-files[0]=rules/discount.drl",
                "framework.drools.rule.rule-files[1]=rules/risk.drl")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RuleEngine engine = context.getBean(RuleEngine.class);
                    assertThat(engine.getRuleCount()).isEqualTo(2);

                    DroolsProperties properties = context.getBean(DroolsProperties.class);
                    assertThat(properties.getRule().getRuleFiles()).containsExactly("rules/discount.drl", "rules/risk.drl");
                });
    }

    @Test
    @DisplayName("自定义会话和治理配置应正确绑定")
    void customSessionAndGovernancePropertiesShouldBindCorrectly() {
        contextRunner.withPropertyValues(
                "framework.drools.session.max-sessions=50",
                "framework.drools.session.session-timeout=60000",
                "framework.drools.governance.audit-enabled=false",
                "framework.drools.governance.max-execution-time-ms=10000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DroolsProperties properties = context.getBean(DroolsProperties.class);
                    assertThat(properties.getSession().getMaxSessions()).isEqualTo(50);
                    assertThat(properties.getSession().getSessionTimeout()).isEqualTo(60000L);
                    assertThat(properties.getGovernance().getAuditEnabled()).isFalse();
                    assertThat(properties.getGovernance().getMaxExecutionTimeMs()).isEqualTo(10000L);
                });
    }

    @Test
    @DisplayName("用户提供的 RuleEngine 应覆盖默认 Bean")
    void userProvidedRuleEngineShouldOverrideDefault() {
        RuleEngine customEngine = new RuleEngine() {
            @Override
            public com.microservice.framework.drools.api.RuleExecutionResult execute(java.util.Collection<?> facts) {
                return com.microservice.framework.drools.api.RuleExecutionResult.empty(0);
            }

            @Override
            public com.microservice.framework.drools.api.RuleExecutionResult executeWithFacts(java.util.Collection<?> facts, java.util.Map<String, Object> globals) {
                return com.microservice.framework.drools.api.RuleExecutionResult.empty(0);
            }

            @Override
            public boolean validate() {
                return true;
            }

            @Override
            public int getRuleCount() {
                return 99;
            }

            @Override
            public java.util.List<String> getRuleGroup(String group) {
                return java.util.Collections.emptyList();
            }
        };

        contextRunner.withBean("customRuleEngine", RuleEngine.class, () -> customEngine)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("customRuleEngine");
                    assertThat(context).doesNotHaveBean("ruleEngine");
                    assertThat(context.getBean(RuleEngine.class).getRuleCount()).isEqualTo(99);
                });
    }
}
