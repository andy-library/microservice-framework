package com.microservice.framework.xxljob.api;

/**
 * 幂等任务处理器接口
 * <p>
 * 在 {@link JobHandler} 基础上增加幂等保护，确保同一任务不会被重复执行。
 * <p>
 * 幂等检查基于 jobId + executorId 组合生成唯一标识，首次执行时标记为已处理，
 * 重复触发时直接返回成功结果并附带"重复执行已跳过"的消息。
 * <p>
 * 使用示例：
 * <pre>
 * JobHandler rawHandler = new OrderSyncJobHandler();
 * IdempotentJobHandler idempotentHandler = new IdempotentJobHandlerDecorator(rawHandler, store);
 *
 * // 框架自动包装，业务无需手动处理
 * JobResult result = idempotentHandler.execute(context);
 * </pre>
 *
 * @author Andy Yang
 */
public interface IdempotentJobHandler {

    /**
     * 判断任务是否为重复执行
     * <p>
     * 基于 jobId + executorId 组合生成唯一标识，
     * 查询存储判断是否已执行过此任务。
     *
     * @param context 任务执行上下文
     * @return true 表示任务已被执行过（重复），应跳过
     */
    boolean isDuplicate(JobExecutionContext context);

    /**
     * 标记任务为已执行
     * <p>
     * 任务成功完成后调用此方法，将唯一标识写入存储，
     * 后续相同任务将被 {@link #isDuplicate} 识别为重复。
     *
     * @param context 任务执行上下文
     */
    void markProcessed(JobExecutionContext context);

    /**
     * 执行幂等保护的任务
     * <p>
     * 先通过 {@link #isDuplicate} 检查是否重复，
     * 若重复则直接返回 {@link JobResult#success(String)} 并附带跳过消息；
     * 若首次执行则调用原始 {@link JobHandler#execute}，
     * 成功后调用 {@link #markProcessed} 标记为已执行。
     *
     * @param context 任务执行上下文
     * @return 任务执行结果
     */
    JobResult execute(JobExecutionContext context);

    /**
     * 获取被装饰的原始 JobHandler
     *
     * @return 原始任务处理器
     */
    JobHandler getDelegate();
}
