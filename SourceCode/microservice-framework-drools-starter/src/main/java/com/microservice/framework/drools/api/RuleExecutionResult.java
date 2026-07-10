package com.microservice.framework.drools.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则执行结果
 * <p>
 * 不可变对象，封装规则引擎执行后的所有输出信息。
 * 包括匹配的规则列表、触发的规则列表、输出事实对象和执行耗时。
 * <p>
 * 匹配规则（matchedRules）：规则条件满足但可能未触发
 * 触发规则（firedRules）：实际执行了动作的规则
 * 输出事实（outputFacts）：规则执行后修改或新增的事实对象
 *
 * @author Andy Yang
 */
public final class RuleExecutionResult {

    private final List<String> matchedRules;
    private final List<String> firedRules;
    private final List<Object> outputFacts;
    private final long executionTimeMs;

    /**
     * 创建规则执行结果
     *
     * @param matchedRules    匹配的规则名称列表
     * @param firedRules      触发的规则名称列表
     * @param outputFacts     输出事实对象列表
     * @param executionTimeMs 执行耗时（毫秒）
     */
    public RuleExecutionResult(List<String> matchedRules, List<String> firedRules,
                               List<Object> outputFacts, long executionTimeMs) {
        this.matchedRules = Collections.unmodifiableList(new ArrayList<>(matchedRules));
        this.firedRules = Collections.unmodifiableList(new ArrayList<>(firedRules));
        this.outputFacts = Collections.unmodifiableList(new ArrayList<>(outputFacts));
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * 创建空的规则执行结果
     *
     * @param executionTimeMs 执行耗时（毫秒）
     * @return 空的执行结果
     */
    public static RuleExecutionResult empty(long executionTimeMs) {
        return new RuleExecutionResult(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                executionTimeMs);
    }

    /**
     * 获取匹配的规则名称列表
     * <p>
     * 规则条件满足但可能未触发执行的规则列表。
     *
     * @return 不可变的匹配规则名称列表
     */
    public List<String> getMatchedRules() {
        return matchedRules;
    }

    /**
     * 获取触发的规则名称列表
     * <p>
     * 实际执行了动作的规则列表。
     *
     * @return 不可变的触发规则名称列表
     */
    public List<String> getFiredRules() {
        return firedRules;
    }

    /**
     * 获取输出事实对象列表
     * <p>
     * 规则执行后修改或新增的事实对象。
     *
     * @return 不可变的输出事实对象列表
     */
    public List<Object> getOutputFacts() {
        return outputFacts;
    }

    /**
     * 获取规则执行耗时
     *
     * @return 执行耗时（毫秒）
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * 是否有任何规则被触发
     *
     * @return 是否有触发的规则
     */
    public boolean hasFiredRules() {
        return !firedRules.isEmpty();
    }

    /**
     * 是否有任何规则匹配
     *
     * @return 是否有匹配的规则
     */
    public boolean hasMatchedRules() {
        return !matchedRules.isEmpty();
    }

    @Override
    public String toString() {
        return "RuleExecutionResult{" +
                "matchedRules=" + matchedRules +
                ", firedRules=" + firedRules +
                ", outputFactsCount=" + outputFacts.size() +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RuleExecutionResult)) {
            return false;
        }
        RuleExecutionResult that = (RuleExecutionResult) o;
        return executionTimeMs == that.executionTimeMs
                && matchedRules.equals(that.matchedRules)
                && firedRules.equals(that.firedRules)
                && outputFacts.equals(that.outputFacts);
    }

    @Override
    public int hashCode() {
        int result = matchedRules.hashCode();
        result = 31 * result + firedRules.hashCode();
        result = 31 * result + outputFacts.hashCode();
        result = 31 * result + Long.hashCode(executionTimeMs);
        return result;
    }
}
