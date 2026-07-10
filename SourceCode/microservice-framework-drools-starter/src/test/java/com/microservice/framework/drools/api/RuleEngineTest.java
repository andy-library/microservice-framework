package com.microservice.framework.drools.api;

import com.microservice.framework.drools.DroolsProperties;
import com.microservice.framework.drools.autoconfigure.DroolsAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("RuleEngine execute 应返回空结果")
    void ruleEngineExecuteShouldReturnEmptyResult() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            RuleExecutionResult result = engine.execute(Collections.singletonList("fact"));

            assertThat(result).isNotNull();
            assertThat(result.getMatchedRules()).isEmpty();
            assertThat(result.getFiredRules()).isEmpty();
            assertThat(result.getOutputFacts()).isEmpty();
        });
    }

    @Test
    @DisplayName("RuleEngine executeWithFacts 应返回空结果")
    void ruleEngineExecuteWithFactsShouldReturnEmptyResult() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            RuleExecutionResult result = engine.executeWithFacts(
                    Collections.singletonList("fact"),
                    Collections.singletonMap("key", "value"));

            assertThat(result).isNotNull();
            assertThat(result.hasFiredRules()).isFalse();
            assertThat(result.hasMatchedRules()).isFalse();
        });
    }

    @Test
    @DisplayName("RuleEngine validate 应返回 true")
    void ruleEngineValidateShouldReturnTrue() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            assertThat(engine.validate()).isTrue();
        });
    }

    @Test
    @DisplayName("RuleEngine getRuleCount 应返回配置的规则文件数")
    void ruleEngineGetRuleCountShouldReflectConfiguredFiles() {
        contextRunner.withPropertyValues(
                "framework.drools.rule.rule-files[0]=rules/discount.drl",
                "framework.drools.rule.rule-files[1]=rules/risk.drl",
                "framework.drools.rule.rule-files[2]=rules/pricing.drl")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RuleEngine engine = context.getBean(RuleEngine.class);
                    assertThat(engine.getRuleCount()).isEqualTo(3);
                });
    }

    @Test
    @DisplayName("RuleEngine getRuleGroup 应返回空列表")
    void ruleEngineGetRuleGroupShouldReturnEmptyList() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            assertThat(engine.getRuleGroup("discount")).isEmpty();
        });
    }

    @Test
    @DisplayName("无规则文件时 getRuleCount 应返回零")
    void noRuleFilesShouldReturnZeroRuleCount() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RuleEngine engine = context.getBean(RuleEngine.class);
            assertThat(engine.getRuleCount()).isEqualTo(0);
        });
    }
}
