package com.microservice.framework.drools.api;

import com.microservice.framework.drools.DroolsProperties;
import com.microservice.framework.drools.autoconfigure.DroolsAutoConfiguration;
import com.microservice.framework.drools.support.Applicant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RuleEngine 接口测试
 * <p>
 * 通过 ApplicationContextRunner 验证 RuleEngine 接口的行为，
 * 确保默认实现满足接口契约。
 *
 * @author Andy Yang
 */
class RuleEngineTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DroolsAutoConfiguration.class));

    @Test
    @DisplayName("RuleEngine execute 应编译并触发配置的 DRL 规则")
    void ruleEngineExecuteShouldCompileAndFireConfiguredDrlRules() {
        contextRunner.withPropertyValues("framework.drools.rule.rule-files[0]=rules/eligibility.drl")
                .run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            Applicant applicant = new Applicant("Ada", 21);
            RuleExecutionResult result = engine.execute(Collections.singletonList(applicant));

            assertThat(result).isNotNull();
            assertThat(result.getMatchedRules()).containsExactly("Adult applicant approved");
            assertThat(result.getFiredRules()).containsExactly("Adult applicant approved");
            assertThat(result.getOutputFacts()).containsExactly(applicant);
            assertThat(applicant.isEligible()).isTrue();
        });
    }

    @Test
    @DisplayName("RuleEngine executeWithFacts 应向规则提供全局变量")
    void ruleEngineExecuteWithFactsShouldProvideGlobalsToRules() {
        contextRunner.withPropertyValues("framework.drools.rule.rule-files[0]=rules/eligibility.drl")
                .run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            List<String> auditMessages = new ArrayList<>();

            RuleExecutionResult result = engine.executeWithFacts(
                    Collections.singletonList(new Applicant("Grace", 19)),
                    Collections.singletonMap("auditMessages", auditMessages));

            assertThat(result).isNotNull();
            assertThat(result.hasFiredRules()).isTrue();
            assertThat(result.hasMatchedRules()).isTrue();
            assertThat(auditMessages).containsExactly("Grace approved");
        });
    }

    @Test
    @DisplayName("RuleEngine validate 应返回 true")
    void ruleEngineValidateShouldReturnTrue() {
        contextRunner.withPropertyValues("framework.drools.rule.rule-files[0]=rules/eligibility.drl")
                .run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            assertThat(engine.validate()).isTrue();
        });
    }

    @Test
    @DisplayName("RuleEngine getRuleCount 应返回已编译规则数")
    void ruleEngineGetRuleCountShouldReflectCompiledRules() {
        contextRunner.withPropertyValues(
                "framework.drools.rule.rule-files[0]=rules/eligibility.drl")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RuleEngine engine = context.getBean(RuleEngine.class);
                    assertThat(engine.getRuleCount()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("RuleEngine getRuleGroup 应返回规则元数据分组中的规则名称")
    void ruleEngineGetRuleGroupShouldReturnRulesByMetadataGroup() {
        contextRunner.withPropertyValues("framework.drools.rule.rule-files[0]=rules/eligibility.drl")
                .run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            assertThat(engine.getRuleGroup("eligibility")).containsExactly("Adult applicant approved");
            assertThat(engine.getRuleGroup("missing")).isEmpty();
        });
    }

    @Test
    @DisplayName("启动校验关闭时非法规则可延迟到 validate 和 execute 暴露")
    void invalidRulesShouldBeReportedWhenStartupValidationIsDisabled() {
        contextRunner.withPropertyValues(
                "framework.drools.rule.rule-files[0]=rules/invalid-rule.drl",
                "framework.drools.rule.validate-on-startup=false")
                .run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            assertThat(engine.getRuleCount()).isEqualTo(0);
            assertThat(engine.validate()).isFalse();
            assertThatThrownBy(() -> engine.execute(Collections.singletonList(new Applicant("Linus", 42))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Drools rule compilation failed");
        });
    }
}
