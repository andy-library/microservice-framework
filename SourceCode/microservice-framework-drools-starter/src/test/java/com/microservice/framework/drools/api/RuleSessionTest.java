package com.microservice.framework.drools.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RuleSession 接口测试
 * <p>
 * 验证 RuleSession 接口的方法签名和基本行为约束。
 *
 * @author Andy Yang
 */
class RuleSessionTest {

    @Test
    @DisplayName("RuleSession insert 应返回事实句柄")
    void ruleSessionInsertShouldReturnFactHandle() {
        RuleSession session = new TestRuleSession();
        Object handle = session.insert("testFact");
        assertThat(handle).isNotNull();
    }

    @Test
    @DisplayName("RuleSession insertAll 应批量插入事实对象")
    void ruleSessionInsertAllShouldInsertMultipleFacts() {
        RuleSession session = new TestRuleSession();
        session.insertAll(List.of("fact1", "fact2", "fact3"));
        assertThat(((TestRuleSession) session).getInsertedFacts().size()).isEqualTo(3);
    }

    @Test
    @DisplayName("RuleSession execute 后应返回执行结果")
    void ruleSessionExecuteShouldReturnResult() {
        RuleSession session = new TestRuleSession();
        session.insert("fact1");
        RuleExecutionResult result = session.execute();
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("RuleSession getResult 应返回最后一次执行结果")
    void ruleSessionGetResultShouldReturnLastResult() {
        RuleSession session = new TestRuleSession();
        RuleExecutionResult result = session.getResult();
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("RuleSession dispose 应释放资源")
    void ruleSessionDisposeShouldReleaseResources() {
        RuleSession session = new TestRuleSession();
        session.dispose();
        assertThat(((TestRuleSession) session).isDisposed()).isTrue();
    }

    /**
     * 测试用的 RuleSession 实现
     */
    static class TestRuleSession implements RuleSession {

        private final List<Object> insertedFacts = new ArrayList<>();
        private boolean disposed = false;
        private RuleExecutionResult lastResult = RuleExecutionResult.empty(0);

        List<Object> getInsertedFacts() {
            return insertedFacts;
        }

        boolean isDisposed() {
            return disposed;
        }

        @Override
        public Object insert(Object fact) {
            insertedFacts.add(fact);
            return fact;
        }

        @Override
        public void insertAll(java.util.Collection<?> facts) {
            insertedFacts.addAll(facts);
        }

        @Override
        public RuleExecutionResult execute() {
            lastResult = RuleExecutionResult.empty(1);
            return lastResult;
        }

        @Override
        public RuleExecutionResult getResult() {
            return lastResult;
        }

        @Override
        public void dispose() {
            disposed = true;
            insertedFacts.clear();
        }
    }
}
