package com.microservice.framework.async.api;

import com.microservice.framework.async.AsyncProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ThreadPoolProfile 创建和验证测试
 * <p>
 * 验证从配置属性创建档案、静态工厂方法创建档案，
 * 以及参数校验逻辑。
 *
 * @author Andy Yang
 */
class ThreadPoolProfileTest {

    @Test
    @DisplayName("从 PoolProperties 创建 ThreadPoolProfile 应保留所有属性")
    void creatingFromPoolPropertiesShouldRetainAllProperties() {
        AsyncProperties.PoolProperties poolProps = new AsyncProperties.PoolProperties();
        poolProps.setCoreSize(8);
        poolProps.setMaxSize(16);
        poolProps.setQueueCapacity(512);
        poolProps.setThreadNamePrefix("worker-");
        poolProps.setRejection(AsyncProperties.RejectionPolicy.ABORT);

        ThreadPoolProfile profile = ThreadPoolProfile.of(poolProps);

        assertThat(profile.getCoreSize()).isEqualTo(8);
        assertThat(profile.getMaxSize()).isEqualTo(16);
        assertThat(profile.getQueueCapacity()).isEqualTo(512);
        assertThat(profile.getThreadNamePrefix()).isEqualTo("worker-");
        assertThat(profile.getRejectionPolicy()).isEqualTo(AsyncProperties.RejectionPolicy.ABORT);
    }

    @Test
    @DisplayName("静态工厂方法创建 ThreadPoolProfile 应保留所有属性")
    void staticFactoryMethodShouldCreateProfileWithAllProperties() {
        ThreadPoolProfile profile = ThreadPoolProfile.of(
                4, 8, 256, "async-", AsyncProperties.RejectionPolicy.CALLER_RUNS);

        assertThat(profile.getCoreSize()).isEqualTo(4);
        assertThat(profile.getMaxSize()).isEqualTo(8);
        assertThat(profile.getQueueCapacity()).isEqualTo(256);
        assertThat(profile.getThreadNamePrefix()).isEqualTo("async-");
        assertThat(profile.getRejectionPolicy()).isEqualTo(AsyncProperties.RejectionPolicy.CALLER_RUNS);
    }

    @Test
    @DisplayName("默认 PoolProperties 创建的 ThreadPoolProfile 应与设计规格一致")
    void defaultPoolPropertiesShouldMatchSpecification() {
        AsyncProperties.PoolProperties poolProps = new AsyncProperties.PoolProperties();
        ThreadPoolProfile profile = ThreadPoolProfile.of(poolProps);

        assertThat(profile.getCoreSize()).isEqualTo(4);
        assertThat(profile.getMaxSize()).isEqualTo(8);
        assertThat(profile.getQueueCapacity()).isEqualTo(256);
        assertThat(profile.getThreadNamePrefix()).isEqualTo("async-");
        assertThat(profile.getRejectionPolicy()).isEqualTo(AsyncProperties.RejectionPolicy.CALLER_RUNS);
    }

    @Test
    @DisplayName("coreSize 小于 1 时应抛出 IllegalArgumentException")
    void coreSizeLessThanOneShouldThrow() {
        assertThatThrownBy(() -> ThreadPoolProfile.of(0, 8, 256, "async-", AsyncProperties.RejectionPolicy.CALLER_RUNS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coreSize must be at least 1");
    }

    @Test
    @DisplayName("maxSize 小于 coreSize 时应抛出 IllegalArgumentException")
    void maxSizeLessThanCoreSizeShouldThrow() {
        assertThatThrownBy(() -> ThreadPoolProfile.of(8, 4, 256, "async-", AsyncProperties.RejectionPolicy.CALLER_RUNS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxSize must be at least coreSize");
    }

    @Test
    @DisplayName("queueCapacity 为负数时应抛出 IllegalArgumentException")
    void negativeQueueCapacityShouldThrow() {
        assertThatThrownBy(() -> ThreadPoolProfile.of(4, 8, -1, "async-", AsyncProperties.RejectionPolicy.CALLER_RUNS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("queueCapacity must be at least 0");
    }

    @Test
    @DisplayName("threadNamePrefix 为空时应抛出 IllegalArgumentException")
    void blankThreadNamePrefixShouldThrow() {
        assertThatThrownBy(() -> ThreadPoolProfile.of(4, 8, 256, "", AsyncProperties.RejectionPolicy.CALLER_RUNS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threadNamePrefix must not be null or blank");
    }

    @Test
    @DisplayName("threadNamePrefix 为 null 时应抛出 IllegalArgumentException")
    void nullThreadNamePrefixShouldThrow() {
        assertThatThrownBy(() -> ThreadPoolProfile.of(4, 8, 256, null, AsyncProperties.RejectionPolicy.CALLER_RUNS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threadNamePrefix must not be null or blank");
    }

    @Test
    @DisplayName("ABORT 拒绝策略应转换为 AbortPolicy")
    void abortPolicyShouldConvertToAbortPolicy() {
        ThreadPoolProfile profile = ThreadPoolProfile.of(4, 8, 256, "async-", AsyncProperties.RejectionPolicy.ABORT);
        assertThat(profile.toRejectedExecutionHandler())
                .isInstanceOf(java.util.concurrent.ThreadPoolExecutor.AbortPolicy.class);
    }

    @Test
    @DisplayName("CALLER_RUNS 拒绝策略应转换为 CallerRunsPolicy")
    void callerRunsPolicyShouldConvertToCallerRunsPolicy() {
        ThreadPoolProfile profile = ThreadPoolProfile.of(4, 8, 256, "async-", AsyncProperties.RejectionPolicy.CALLER_RUNS);
        assertThat(profile.toRejectedExecutionHandler())
                .isInstanceOf(java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy.class);
    }

    @Test
    @DisplayName("DISCARD_OLDEST 拒绝策略应转换为 DiscardOldestPolicy")
    void discardOldestPolicyShouldConvertToDiscardOldestPolicy() {
        ThreadPoolProfile profile = ThreadPoolProfile.of(4, 8, 256, "async-", AsyncProperties.RejectionPolicy.DISCARD_OLDEST);
        assertThat(profile.toRejectedExecutionHandler())
                .isInstanceOf(java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy.class);
    }

    @Test
    @DisplayName("相同属性的 ThreadPoolProfile 应相等")
    void equalProfilesShouldBeEqual() {
        ThreadPoolProfile profile1 = ThreadPoolProfile.of(4, 8, 256, "async-", AsyncProperties.RejectionPolicy.CALLER_RUNS);
        ThreadPoolProfile profile2 = ThreadPoolProfile.of(4, 8, 256, "async-", AsyncProperties.RejectionPolicy.CALLER_RUNS);

        assertThat(profile1).isEqualTo(profile2);
        assertThat(profile1.hashCode()).isEqualTo(profile2.hashCode());
    }

    @Test
    @DisplayName("不同属性的 ThreadPoolProfile 应不相等")
    void differentProfilesShouldNotBeEqual() {
        ThreadPoolProfile profile1 = ThreadPoolProfile.of(4, 8, 256, "async-", AsyncProperties.RejectionPolicy.CALLER_RUNS);
        ThreadPoolProfile profile2 = ThreadPoolProfile.of(4, 16, 256, "async-", AsyncProperties.RejectionPolicy.CALLER_RUNS);

        assertThat(profile1).isNotEqualTo(profile2);
    }
}
