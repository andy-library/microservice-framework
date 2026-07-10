package com.microservice.framework.kafka.api;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * 死信消息处理接口
 * <p>
 * 提供死信消息接收和重试能力。当消息消费失败超过最大重试次数后，
 * 将被转发到死信主题（原始主题 + topicSuffix），
 * 由 {@link DeadLetterHandler} 负责记录、告警或人工介入。
 *
 * @param <K> 消息 Key 类型
 * @param <V> 消息 Value 类型
 * @author Andy Yang
 */
public interface DeadLetterHandler<K, V> {

    /**
     * 处理死信消息
     * <p>
     * 接收到死信主题的消息后调用此方法，通常用于：
     * - 记录死信消息到持久化存储
     * - 发送告警通知
     * - 触发人工补偿流程
     *
     * @param record 死信消息记录
     */
    void handle(ConsumerRecord<K, V> record);

    /**
     * 重试死信消息
     * <p>
     * 将死信消息重新发送到原始主题进行消费重试。
     * 调用前应检查当前重试次数不超过 {@link #getMaxRetries()}。
     *
     * @param record      死信消息记录
     * @param retryCount  当前重试次数
     * @return true 表示重试发送成功，false 表示超过最大重试次数或发送失败
     */
    boolean retry(ConsumerRecord<K, V> record, int retryCount);

    /**
     * 获取最大重试次数
     * <p>
     * 重试次数超过此值后，消息不再重试，仅由 {@link #handle} 处理。
     *
     * @return 最大重试次数
     */
    int getMaxRetries();
}
