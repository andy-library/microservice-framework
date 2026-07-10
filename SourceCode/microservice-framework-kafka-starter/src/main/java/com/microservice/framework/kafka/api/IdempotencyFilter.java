package com.microservice.framework.kafka.api;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * 幂等消费过滤接口
 * <p>
 * 用于检测和标记已处理的消息，防止重复消费产生副作用。
 * <p>
 * 典型实现基于 Redis、数据库或本地缓存，通过消息的唯一标识
 * （如 topic + partition + offset 组合）来判断是否已被处理。
 * <p>
 * 使用示例：
 * <pre>
 * IdempotencyFilter filter = ...;
 * if (filter.isDuplicate(record)) {
 *     // 跳过重复消息
 *     return;
 * }
 * // 处理消息
 * process(record);
 * // 标记为已处理
 * filter.markProcessed(record);
 * </pre>
 *
 * @param <K> 消息 Key 类型
 * @param <V> 消息 Value 类型
 * @author Andy Yang
 */
public interface IdempotencyFilter<K, V> {

    /**
     * 判断消息是否为重复消息
     * <p>
     * 基于 topic + partition + offset 组合生成唯一标识，
     * 查询存储判断是否已处理过此消息。
     *
     * @param record 消费者记录
     * @return true 表示消息已被处理过（重复），应跳过
     */
    boolean isDuplicate(ConsumerRecord<K, V> record);

    /**
     * 标记消息为已处理
     * <p>
     * 消息成功处理后调用此方法，将唯一标识写入存储，
     * 后续相同消息将被 {@link #isDuplicate} 识别为重复。
     *
     * @param record 消费者记录
     */
    void markProcessed(ConsumerRecord<K, V> record);
}
