package com.microservice.framework.database.api;

/**
 * 幂等记录存储
 * <p>
 * 提供幂等性保障能力，防止重复请求导致的业务副作用。
 * 通过在数据库中记录已处理的请求标识，实现请求去重。
 * <p>
 * 设计意图：
 * <ul>
 *   <li>适用于支付、转账等不可重复执行的核心业务</li>
 *   <li>基于数据库实现，天然具备事务一致性</li>
 *   <li>配合 TransactionTemplateFacade 使用，幂等检查与业务操作在同一事务内</li>
 * </ul>
 * <p>
 * 使用流程：
 * <ol>
 *   <li>业务入口调用 {@link #exists(String)} 判断请求是否已处理</li>
 *   <li>若不存在，执行业务操作并调用 {@link #mark(String, String)} 记录幂等标识</li>
 *   <li>若存在，直接返回上次的处理结果（从 mark 时存储的 payload 中获取）</li>
 * </ol>
 *
 * @author Andy Yang
 */
public interface IdempotencyStore {

    /**
     * 判断幂等标识是否已存在
     * <p>
     * 存在意味着该请求已被处理过，业务应跳过重复执行。
     *
     * @param idempotencyKey 幂等标识（如请求 ID、业务流水号）
     * @return 如果标识已存在返回 true
     */
    boolean exists(String idempotencyKey);

    /**
     * 记录幂等标识并附带处理结果
     * <p>
     * 在业务操作成功后调用，将幂等标识和处理结果持久化。
     * 后续相同标识的请求可通过 payload 获取上次的处理结果。
     *
     * @param idempotencyKey 幂等标识
     * @param payload        处理结果的 JSON 表示，用于重复请求时直接返回
     */
    void mark(String idempotencyKey, String payload);

    /**
     * 移除幂等记录
     * <p>
     * 用于超时清理或业务回滚场景，移除后相同标识的请求可重新处理。
     *
     * @param idempotencyKey 幂等标识
     */
    void remove(String idempotencyKey);
}
