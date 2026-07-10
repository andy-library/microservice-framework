package com.microservice.framework.drools.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RuleExecutionResult 不可变性和构造测试
 * <p>
 * 验证 RuleExecutionResult 的不可变性、空结果工厂方法和基本属性。
 *
 * @author Andy Yang
 */
class RuleExecutionResultTest {

    @Test
    @DisplayName("RuleExecutionResult 应为不可变对象")
    void ruleExecutionResultShouldBeImmutable() {
        List<String> matchedRules = new ArrayList<>(Arrays.asList("rule1", "rule2"));
        List<String> firedRules = new ArrayList<>(Arrays.asList("rule1"));
        List<Object> outputFacts = new ArrayList<>(Arrays.asList("fact1"));

        RuleExecutionResult result = new RuleExecutionResult(matchedRules, firedRules, outputFacts, 100);

        // 修改原始列表不应影响结果对象
        matchedRules.add("rule3");
        firedRules.add("rule2");
        outputFacts.add("fact2");

        assertThat(result.getMatchedRules()).containsExactly("rule1", "rule2");
        assertThat(result.getFiredRules()).containsExactly("rule1");
        assertThat(result.getOutputFacts()).containsExactly("fact1");
    }

    @Test
    @DisplayName("RuleExecutionResult 的列表应为不可修改")
    void ruleExecutionResultListsShouldBeUnmodifiable() {
        RuleExecutionResult result = new RuleExecutionResult(
                Arrays.asList("rule1"), Arrays.asList("rule1"), Arrays.asList("fact1"), 50);

        assertThatThrownBy(() -> result.getMatchedRules().add("rule2"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.getFiredRules().add("rule2"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.getOutputFacts().add("fact2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("empty 工厂方法应创建空结果")
    void emptyFactoryMethodShouldCreateEmptyResult() {
        RuleExecutionResult result = RuleExecutionResult.empty(25);

        assertThat(result.getMatchedRules()).isEmpty();
        assertThat(result.getFiredRules()).isEmpty();
        assertThat(result.getOutputFacts()).isEmpty();
        assertThat(result.getExecutionTimeMs()).isEqualTo(25);
        assertThat(result.hasFiredRules()).isFalse();
        assertThat(result.hasMatchedRules()).isFalse();
    }

    @Test
    @DisplayName("hasFiredRules 和 hasMatchedRules 应正确判断")
    void hasFiredRulesAndHasMatchedRulesShouldReturnCorrectly() {
        RuleExecutionResult emptyResult = RuleExecutionResult.empty(0);
        assertThat(emptyResult.hasFiredRules()).isFalse();
        assertThat(emptyResult.hasMatchedRules()).isFalse();

        RuleExecutionResult nonEmptyResult = new RuleExecutionResult(
                Arrays.asList("rule1"), Arrays.asList("rule1"), Collections.emptyList(), 100);
        assertThat(nonEmptyResult.hasFiredRules()).isTrue();
        assertThat(nonEmptyResult.hasMatchedRules()).isTrue();
    }

    @Test
    @DisplayName("RuleExecutionResult toString 应包含关键信息")
    void ruleExecutionResultToStringShouldContainKeyInfo() {
        RuleExecutionResult result = new RuleExecutionResult(
                Arrays.asList("rule1"), Arrays.asList("rule1"), Arrays.asList("fact1"), 100);

        String str = result.toString();
        assertThat(str).contains("RuleExecutionResult");
        assertThat(str).contains("executionTimeMs=100");
        assertThat(str).contains("outputFactsCount=1");
    }

    @Test
    @DisplayName("RuleExecutionResult equals 和 hashCode 应正确工作")
    void ruleExecutionResultEqualsAndHashCodeShouldWork() {
        RuleExecutionResult result1 = new RuleExecutionResult(
                Arrays.asList("rule1"), Arrays.asList("rule1"), Arrays.asList("fact1"), 100);
        RuleExecutionResult result2 = new RuleExecutionResult(
                Arrays.asList("rule1"), Arrays.asList("rule1"), Arrays.asList("fact1"), 100);
        RuleExecutionResult result3 = new RuleExecutionResult(
                Arrays.asList("rule2"), Arrays.asList("rule2"), Arrays.asList("fact2"), 200);

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        assertThat(result1).isNotEqualTo(result3);
    }
}
