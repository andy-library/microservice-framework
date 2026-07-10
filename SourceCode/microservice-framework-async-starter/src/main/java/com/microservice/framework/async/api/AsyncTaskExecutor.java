package com.microservice.framework.async.api;

import org.springframework.core.task.TaskExecutor;

/**
 * 治理异步任务执行器接口
 * <p>
 * 扩展 Spring {@link TaskExecutor}，增加线程池监控能力，
 * 使运维团队可以实时观测线程池的健康状态。
 * <p>
 * 监控指标可用于：
 * - 健康检查（活跃线程数是否接近最大值）
 * - 告警（队列积压超过阈值）
 * - 自适应调整（根据负载动态调整线程数）
 *
 * @author Andy Yang
 */
public interface AsyncTaskExecutor extends TaskExecutor {

    /**
     * 获取当前活跃线程数
     * <p>
     * 活跃线程正在执行任务，此值接近 maxSize 表示线程池接近饱和。
     *
     * @return 当前活跃执行任务的线程数
     */
    int getActiveCount();

    /**
     * 获取当前线程池大小
     * <p>
     * 包含核心线程和临时创建的额外线程。
     *
     * @return 当前线程池中的线程总数
     */
    int getPoolSize();

    /**
     * 获取当前队列中等待执行的任务数
     * <p>
     * 队列积压过多表示任务提交速度超过处理速度。
     *
     * @return 队列中等待的任务数量
     */
    int getQueueSize();

    /**
     * 获取已完成任务总数
     * <p>
     * 用于统计吞吐量和任务完成率。
     *
     * @return 线程池已完成任务的累计数量
     */
    long getCompletedTaskCount();
}
