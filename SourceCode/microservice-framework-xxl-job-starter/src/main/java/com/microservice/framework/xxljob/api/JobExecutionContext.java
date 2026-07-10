package com.microservice.framework.xxljob.api;

import java.time.Instant;
import java.util.Objects;

/**
 * 任务执行上下文
 * <p>
 * 不可变上下文对象，封装 XXL-JOB 分发任务时的所有元信息，
 * 包括任务 ID、执行器 ID、参数、分片索引和触发时间等。
 * <p>
 * 上下文在任务执行期间由框架注入，不可被业务修改。
 *
 * @author Andy Yang
 */
public final class JobExecutionContext {

    /**
     * 任务 ID（XXL-JOB 的 jobId）
     */
    private final int jobId;

    /**
     * 执行器 ID（XXL-JOB 的 executorId）
     */
    private final String executorId;

    /**
     * 任务参数
     */
    private final String param;

    /**
     * 分片索引（从 0 开始）
     */
    private final int shardIndex;

    /**
     * 分片总数
     */
    private final int shardTotal;

    /**
     * 触发时间
     */
    private final Instant triggerTime;

    /**
     * 全参数构造方法
     *
     * @param jobId       任务 ID
     * @param executorId  执行器 ID
     * @param param       任务参数
     * @param shardIndex  分片索引
     * @param shardTotal  分片总数
     * @param triggerTime 触发时间
     */
    public JobExecutionContext(int jobId, String executorId, String param,
                               int shardIndex, int shardTotal, Instant triggerTime) {
        this.jobId = jobId;
        this.executorId = executorId;
        this.param = param;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
        this.triggerTime = triggerTime;
    }

    /**
     * 创建简化的上下文（无分片信息）
     *
     * @param jobId      任务 ID
     * @param executorId 执行器 ID
     * @param param      任务参数
     * @return 简化上下文（shardIndex=0, shardTotal=1）
     */
    public static JobExecutionContext of(int jobId, String executorId, String param) {
        return new JobExecutionContext(jobId, executorId, param, 0, 1, null);
    }

    /**
     * 创建完整上下文
     *
     * @param jobId       任务 ID
     * @param executorId  执行器 ID
     * @param param       任务参数
     * @param shardIndex  分片索引
     * @param shardTotal  分片总数
     * @param triggerTime 触发时间
     * @return 完整上下文
     */
    public static JobExecutionContext of(int jobId, String executorId, String param,
                                         int shardIndex, int shardTotal, Instant triggerTime) {
        return new JobExecutionContext(jobId, executorId, param, shardIndex, shardTotal, triggerTime);
    }

    // Getters

    /**
     * 获取任务 ID
     *
     * @return 任务 ID
     */
    public int getJobId() {
        return jobId;
    }

    /**
     * 获取执行器 ID
     *
     * @return 执行器 ID
     */
    public String getExecutorId() {
        return executorId;
    }

    /**
     * 获取任务参数
     *
     * @return 任务参数，可为 null
     */
    public String getParam() {
        return param;
    }

    /**
     * 获取分片索引
     *
     * @return 分片索引（从 0 开始）
     */
    public int getShardIndex() {
        return shardIndex;
    }

    /**
     * 获取分片总数
     *
     * @return 分片总数
     */
    public int getShardTotal() {
        return shardTotal;
    }

    /**
     * 获取触发时间
     *
     * @return 触发时间，可为 null
     */
    public Instant getTriggerTime() {
        return triggerTime;
    }

    /**
     * 判断是否为分片任务
     *
     * @return true 表示分片总数大于 1
     */
    public boolean isSharded() {
        return shardTotal > 1;
    }

    @Override
    public String toString() {
        return "JobExecutionContext{" +
                "jobId=" + jobId +
                ", executorId='" + executorId + '\'' +
                ", param='" + param + '\'' +
                ", shardIndex=" + shardIndex +
                ", shardTotal=" + shardTotal +
                ", triggerTime=" + triggerTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobExecutionContext that = (JobExecutionContext) o;
        return jobId == that.jobId &&
                shardIndex == that.shardIndex &&
                shardTotal == that.shardTotal &&
                Objects.equals(executorId, that.executorId) &&
                Objects.equals(param, that.param) &&
                Objects.equals(triggerTime, that.triggerTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, executorId, param, shardIndex, shardTotal, triggerTime);
    }
}
