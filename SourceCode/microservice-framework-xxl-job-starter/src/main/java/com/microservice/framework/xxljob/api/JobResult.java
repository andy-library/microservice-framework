package com.microservice.framework.xxljob.api;

import java.util.Objects;

/**
 * 任务执行结果
 * <p>
 * 定义任务执行的三种终态：成功、失败和超时，
 * 每种终态附带可选的消息描述。
 * <p>
 * JobResult 是不可变对象，通过静态工厂方法创建：
 * <pre>
 * JobResult result = JobResult.success("处理完成");
 * JobResult result = JobResult.fail("参数校验失败");
 * JobResult result = JobResult.timeout("执行超过 300 秒");
 * </pre>
 *
 * @author Andy Yang
 */
public final class JobResult {

    /**
     * 任务执行状态
     */
    private final Status status;

    /**
     * 结果附带的消息，可为 null
     */
    private final String message;

    /**
     * 私有构造方法，强制使用静态工厂方法创建实例
     *
     * @param status 任务执行状态
     * @param message 结果消息
     */
    private JobResult(Status status, String message) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.message = message;
    }

    /**
     * 创建成功结果
     *
     * @return 成功结果（无消息）
     */
    public static JobResult success() {
        return new JobResult(Status.SUCCESS, null);
    }

    /**
     * 创建带消息的成功结果
     *
     * @param message 成功描述信息
     * @return 成功结果
     */
    public static JobResult success(String message) {
        return new JobResult(Status.SUCCESS, message);
    }

    /**
     * 创建失败结果
     *
     * @return 失败结果（无消息）
     */
    public static JobResult fail() {
        return new JobResult(Status.FAIL, null);
    }

    /**
     * 创建带消息的失败结果
     *
     * @param message 失败原因描述
     * @return 失败结果
     */
    public static JobResult fail(String message) {
        return new JobResult(Status.FAIL, message);
    }

    /**
     * 创建超时结果
     *
     * @return 超时结果（无消息）
     */
    public static JobResult timeout() {
        return new JobResult(Status.TIMEOUT, null);
    }

    /**
     * 创建带消息的超时结果
     *
     * @param message 超时描述信息
     * @return 超时结果
     */
    public static JobResult timeout(String message) {
        return new JobResult(Status.TIMEOUT, message);
    }

    /**
     * 获取任务执行状态
     *
     * @return 执行状态
     */
    public Status getStatus() {
        return status;
    }

    /**
     * 获取结果附带的消息
     *
     * @return 消息描述，可为 null
     */
    public String getMessage() {
        return message;
    }

    /**
     * 判断是否为成功结果
     *
     * @return true 表示任务执行成功
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * 判断是否为失败结果
     *
     * @return true 表示任务执行失败
     */
    public boolean isFail() {
        return status == Status.FAIL;
    }

    /**
     * 判断是否为超时结果
     *
     * @return true 表示任务执行超时
     */
    public boolean isTimeout() {
        return status == Status.TIMEOUT;
    }

    @Override
    public String toString() {
        if (message != null) {
            return status + "(" + message + ")";
        }
        return status.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobResult jobResult = (JobResult) o;
        return status == jobResult.status && Objects.equals(message, jobResult.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, message);
    }

    /**
     * 任务执行状态枚举
     * <p>
     * 定义三种终态：成功、失败和超时。
     */
    public enum Status {
        /**
         * 任务执行成功
         */
        SUCCESS,

        /**
         * 任务执行失败
         */
        FAIL,

        /**
         * 任务执行超时
         */
        TIMEOUT
    }
}
