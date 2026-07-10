package com.microservice.framework.xxljob.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JobResult 构造和状态判断测试
 * <p>
 * 验证工厂方法创建的结果实例状态和消息正确，
 * 以及便捷判断方法（isSuccess、isFail、isTimeout）的行为。
 *
 * @author Andy Yang
 */
class JobResultTest {

    @Test
    @DisplayName("success() 应创建 SUCCESS 状态且无消息的结果")
    void successWithoutMessageShouldCreateSuccessResult() {
        JobResult result = JobResult.success();

        assertThat(result.getStatus()).isEqualTo(JobResult.Status.SUCCESS);
        assertThat(result.getMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFail()).isFalse();
        assertThat(result.isTimeout()).isFalse();
    }

    @Test
    @DisplayName("success(String) 应创建 SUCCESS 状态且带消息的结果")
    void successWithMessageShouldCreateSuccessResultWithMessage() {
        JobResult result = JobResult.success("处理完成");

        assertThat(result.getStatus()).isEqualTo(JobResult.Status.SUCCESS);
        assertThat(result.getMessage()).isEqualTo("处理完成");
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("fail() 应创建 FAIL 状态且无消息的结果")
    void failWithoutMessageShouldCreateFailResult() {
        JobResult result = JobResult.fail();

        assertThat(result.getStatus()).isEqualTo(JobResult.Status.FAIL);
        assertThat(result.getMessage()).isNull();
        assertThat(result.isFail()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isTimeout()).isFalse();
    }

    @Test
    @DisplayName("fail(String) 应创建 FAIL 状态且带消息的结果")
    void failWithMessageShouldCreateFailResultWithMessage() {
        JobResult result = JobResult.fail("参数校验失败");

        assertThat(result.getStatus()).isEqualTo(JobResult.Status.FAIL);
        assertThat(result.getMessage()).isEqualTo("参数校验失败");
        assertThat(result.isFail()).isTrue();
    }

    @Test
    @DisplayName("timeout() 应创建 TIMEOUT 状态且无消息的结果")
    void timeoutWithoutMessageShouldCreateTimeoutResult() {
        JobResult result = JobResult.timeout();

        assertThat(result.getStatus()).isEqualTo(JobResult.Status.TIMEOUT);
        assertThat(result.getMessage()).isNull();
        assertThat(result.isTimeout()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFail()).isFalse();
    }

    @Test
    @DisplayName("timeout(String) 应创建 TIMEOUT 状态且带消息的结果")
    void timeoutWithMessageShouldCreateTimeoutResultWithMessage() {
        JobResult result = JobResult.timeout("执行超过 300 秒");

        assertThat(result.getStatus()).isEqualTo(JobResult.Status.TIMEOUT);
        assertThat(result.getMessage()).isEqualTo("执行超过 300 秒");
        assertThat(result.isTimeout()).isTrue();
    }

    @Test
    @DisplayName("Status 枚举应包含所有定义的状态")
    void statusEnumShouldContainAllDefinedStates() {
        JobResult.Status[] statuses = JobResult.Status.values();

        assertThat(statuses).containsExactlyInAnyOrder(
                JobResult.Status.SUCCESS,
                JobResult.Status.FAIL,
                JobResult.Status.TIMEOUT);
    }

    @Test
    @DisplayName("带消息的结果 toString 应包含状态和消息")
    void toStringWithMessageShouldIncludeStatusAndMessage() {
        JobResult result = JobResult.fail("参数错误");

        assertThat(result.toString()).contains("FAIL");
        assertThat(result.toString()).contains("参数错误");
    }

    @Test
    @DisplayName("无消息的结果 toString 应仅包含状态名称")
    void toStringWithoutMessageShouldIncludeOnlyStatusName() {
        JobResult result = JobResult.success();

        assertThat(result.toString()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("相同状态和消息的 JobResult 应 equals 相等")
    void equalStatusAndMessageShouldBeEqual() {
        JobResult result1 = JobResult.fail("相同原因");
        JobResult result2 = JobResult.fail("相同原因");

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    @DisplayName("不同消息的 JobResult 应 equals 不相等")
    void differentMessageShouldNotBeEqual() {
        JobResult result1 = JobResult.fail("原因 A");
        JobResult result2 = JobResult.fail("原因 B");

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("不同状态的 JobResult 应 equals 不相等")
    void differentStatusShouldNotBeEqual() {
        JobResult result1 = JobResult.success("消息");
        JobResult result2 = JobResult.fail("消息");

        assertThat(result1).isNotEqualTo(result2);
    }
}
