package com.microservice.framework.xxljob.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IdempotentJobHandler 契约测试
 * <p>
 * 使用 stub 实现验证幂等任务处理器接口方法签名和幂等语义，
 * 不依赖真实的 XXL-JOB 集群或外部存储。
 *
 * @author Andy Yang
 */
class IdempotentJobHandlerTest {

    private final IdempotentJobHandler handler = new StubIdempotentJobHandler();

    @Test
    @DisplayName("未执行任务的 isDuplicate 应返回 false")
    void unprocessedJobShouldNotBeDuplicate() {
        JobExecutionContext context = JobExecutionContext.of(1001, "executor-1", "sync");

        assertThat(handler.isDuplicate(context)).isFalse();
    }

    @Test
    @DisplayName("markProcessed 后 isDuplicate 应返回 true")
    void processedJobShouldBeDuplicateAfterMarking() {
        JobExecutionContext context = JobExecutionContext.of(1001, "executor-1", "sync");

        assertThat(handler.isDuplicate(context)).isFalse();
        handler.markProcessed(context);
        assertThat(handler.isDuplicate(context)).isTrue();
    }

    @Test
    @DisplayName("不同 jobId 的任务应独立判断")
    void differentJobIdShouldBeIndependent() {
        JobExecutionContext context1 = JobExecutionContext.of(1001, "executor-1", "sync");
        JobExecutionContext context2 = JobExecutionContext.of(1002, "executor-1", "sync");

        handler.markProcessed(context1);

        assertThat(handler.isDuplicate(context1)).isTrue();
        assertThat(handler.isDuplicate(context2)).isFalse();
    }

    @Test
    @DisplayName("相同 jobId 但不同 executorId 的任务应独立判断")
    void differentExecutorIdShouldBeIndependent() {
        JobExecutionContext context1 = JobExecutionContext.of(1001, "executor-1", "sync");
        JobExecutionContext context2 = JobExecutionContext.of(1001, "executor-2", "sync");

        handler.markProcessed(context1);

        assertThat(handler.isDuplicate(context1)).isTrue();
        assertThat(handler.isDuplicate(context2)).isFalse();
    }

    @Test
    @DisplayName("execute 重复任务时应返回成功结果并附带跳过消息")
    void executingDuplicateJobShouldReturnSuccessWithSkipMessage() {
        JobExecutionContext context = JobExecutionContext.of(1001, "executor-1", "sync");

        // 首次执行
        JobResult firstResult = handler.execute(context);
        assertThat(firstResult.isSuccess()).isTrue();

        // 重复执行
        JobResult duplicateResult = handler.execute(context);
        assertThat(duplicateResult.isSuccess()).isTrue();
        assertThat(duplicateResult.getMessage()).containsIgnoringCase("duplicate");
    }

    @Test
    @DisplayName("getDelegate 应返回原始 JobHandler")
    void getDelegateShouldReturnOriginalHandler() {
        assertThat(handler.getDelegate()).isNotNull();
        assertThat(handler.getDelegate()).isInstanceOf(StubJobHandler.class);
    }

    // ========================================================================
    // Stub 实现
    // ========================================================================

    /**
     * Stub JobHandler 实现
     */
    static class StubJobHandler implements JobHandler {

        @Override
        public JobResult execute(JobExecutionContext context) {
            return JobResult.success("Stub job processed: jobId=" + context.getJobId());
        }
    }

    /**
     * Stub IdempotentJobHandler 实现
     * <p>
     * 使用 ConcurrentHashMap 存储已执行任务的唯一标识。
     */
    static class StubIdempotentJobHandler implements IdempotentJobHandler {

        private final java.util.Set<String> processedKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();
        private final JobHandler delegate = new StubJobHandler();

        @Override
        public boolean isDuplicate(JobExecutionContext context) {
            return processedKeys.contains(buildKey(context));
        }

        @Override
        public void markProcessed(JobExecutionContext context) {
            processedKeys.add(buildKey(context));
        }

        @Override
        public JobResult execute(JobExecutionContext context) {
            if (isDuplicate(context)) {
                return JobResult.success("Duplicate execution skipped for jobId=" + context.getJobId());
            }

            JobResult result = delegate.execute(context);
            if (result.isSuccess()) {
                markProcessed(context);
            }
            return result;
        }

        @Override
        public JobHandler getDelegate() {
            return delegate;
        }

        private String buildKey(JobExecutionContext context) {
            return context.getJobId() + "-" + context.getExecutorId();
        }
    }
}
