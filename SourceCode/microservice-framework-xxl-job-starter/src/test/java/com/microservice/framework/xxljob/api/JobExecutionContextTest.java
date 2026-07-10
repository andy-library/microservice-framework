package com.microservice.framework.xxljob.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JobExecutionContext 创建和属性获取测试
 * <p>
 * 验证上下文的工厂方法、getter、分片判断以及 equals/hashCode 行为。
 *
 * @author Andy Yang
 */
class JobExecutionContextTest {

    @Test
    @DisplayName("of() 简化工厂方法应创建无分片的上下文")
    void simplifiedFactoryMethodShouldCreateContextWithoutSharding() {
        JobExecutionContext context = JobExecutionContext.of(1001, "executor-1", "order-sync");

        assertThat(context.getJobId()).isEqualTo(1001);
        assertThat(context.getExecutorId()).isEqualTo("executor-1");
        assertThat(context.getParam()).isEqualTo("order-sync");
        assertThat(context.getShardIndex()).isEqualTo(0);
        assertThat(context.getShardTotal()).isEqualTo(1);
        assertThat(context.getTriggerTime()).isNull();
        assertThat(context.isSharded()).isFalse();
    }

    @Test
    @DisplayName("of() 完整工厂方法应创建包含所有属性的上下文")
    void fullFactoryMethodShouldCreateContextWithAllProperties() {
        Instant triggerTime = Instant.ofEpochMilli(1700000000000L);
        JobExecutionContext context = JobExecutionContext.of(
                2001, "executor-2", "data-export", 3, 8, triggerTime);

        assertThat(context.getJobId()).isEqualTo(2001);
        assertThat(context.getExecutorId()).isEqualTo("executor-2");
        assertThat(context.getParam()).isEqualTo("data-export");
        assertThat(context.getShardIndex()).isEqualTo(3);
        assertThat(context.getShardTotal()).isEqualTo(8);
        assertThat(context.getTriggerTime()).isEqualTo(triggerTime);
        assertThat(context.isSharded()).isTrue();
    }

    @Test
    @DisplayName("isSharded() 仅在 shardTotal > 1 时返回 true")
    void isShardedShouldReturnTrueOnlyWhenShardTotalGreaterThanOne() {
        JobExecutionContext unsharded = JobExecutionContext.of(1, "e1", "p", 0, 1, null);
        assertThat(unsharded.isSharded()).isFalse();

        JobExecutionContext sharded = JobExecutionContext.of(2, "e2", "p", 0, 2, null);
        assertThat(sharded.isSharded()).isTrue();

        JobExecutionContext shardedLarge = JobExecutionContext.of(3, "e3", "p", 5, 10, null);
        assertThat(shardedLarge.isSharded()).isTrue();
    }

    @Test
    @DisplayName("构造方法应允许 param 和 triggerTime 为 null")
    void constructorShouldAllowNullParamAndTriggerTime() {
        JobExecutionContext context = new JobExecutionContext(1, "e1", null, 0, 1, null);

        assertThat(context.getParam()).isNull();
        assertThat(context.getTriggerTime()).isNull();
    }

    @Test
    @DisplayName("toString 应包含所有属性")
    void toStringShouldIncludeAllProperties() {
        Instant triggerTime = Instant.ofEpochMilli(1700000000000L);
        JobExecutionContext context = JobExecutionContext.of(1, "e1", "p1", 0, 4, triggerTime);

        String str = context.toString();
        assertThat(str).contains("jobId=1");
        assertThat(str).contains("executorId='e1'");
        assertThat(str).contains("param='p1'");
        assertThat(str).contains("shardIndex=0");
        assertThat(str).contains("shardTotal=4");
    }

    @Test
    @DisplayName("相同属性的上下文应 equals 相等")
    void equalPropertiesShouldBeEqual() {
        Instant time = Instant.ofEpochMilli(1700000000000L);
        JobExecutionContext context1 = JobExecutionContext.of(1, "e1", "p", 0, 1, time);
        JobExecutionContext context2 = JobExecutionContext.of(1, "e1", "p", 0, 1, time);

        assertThat(context1).isEqualTo(context2);
        assertThat(context1.hashCode()).isEqualTo(context2.hashCode());
    }

    @Test
    @DisplayName("不同 jobId 的上下文应 equals 不相等")
    void differentJobIdShouldNotBeEqual() {
        JobExecutionContext context1 = JobExecutionContext.of(1, "e1", "p", 0, 1, null);
        JobExecutionContext context2 = JobExecutionContext.of(2, "e1", "p", 0, 1, null);

        assertThat(context1).isNotEqualTo(context2);
    }

    @Test
    @DisplayName("不同 shardIndex 的上下文应 equals 不相等")
    void differentShardIndexShouldNotBeEqual() {
        JobExecutionContext context1 = JobExecutionContext.of(1, "e1", "p", 0, 4, null);
        JobExecutionContext context2 = JobExecutionContext.of(1, "e1", "p", 1, 4, null);

        assertThat(context1).isNotEqualTo(context2);
    }
}
