package com.microservice.framework.xxljob.api;

/**
 * 任务处理器接口
 * <p>
 * 定义 XXL-JOB 任务执行的核心抽象，业务实现此接口并提供
 * {@link com.xxl.job.core.handler.annotation.XxlJob} 注解标记的执行方法。
 * <p>
 * 使用示例：
 * <pre>
 * public class OrderSyncJobHandler implements JobHandler {
 *
 *     &#64;Override
 *     public JobResult execute(JobExecutionContext context) {
 *         // 业务逻辑：根据分片索引处理不同范围的数据
 *         int shardIndex = context.getShardIndex();
 *         int shardTotal = context.getShardTotal();
 *         // ... 执行任务 ...
 *         return JobResult.success("同步完成，处理 " + count + " 条数据");
 *     }
 * }
 * </pre>
 *
 * @author Andy Yang
 */
public interface JobHandler {

    /**
     * 执行任务
     * <p>
     * 框架调用此方法执行任务逻辑。业务实现应：
     * - 根据 {@link JobExecutionContext#getShardIndex()} 和
     *   {@link JobExecutionContext#getShardTotal()} 实现分片处理
     * - 根据 {@link JobExecutionContext#getParam()} 解析任务参数
     * - 返回 {@link JobResult} 表示执行终态
     *
     * @param context 任务执行上下文，包含任务 ID、参数、分片信息等
     * @return 任务执行结果
     */
    JobResult execute(JobExecutionContext context);
}
