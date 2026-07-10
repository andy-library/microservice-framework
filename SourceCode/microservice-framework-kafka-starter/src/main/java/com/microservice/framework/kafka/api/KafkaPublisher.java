package com.microservice.framework.kafka.api;

import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka 消息发布抽象接口
 * <p>
 * 提供单条发送、批量发送、带 Key 发送和指定主题发送能力。
 * 所有方法返回 {@link CompletableFuture}，支持异步非阻塞回调。
 * <p>
 * 默认实现委托给 Spring Kafka 的 {@code KafkaTemplate}。
 *
 * @param <T> 消息体类型
 * @author Andy Yang
 */
public interface KafkaPublisher<T> {

    /**
     * 发送单条消息到默认主题
     *
     * @param data 消息体
     * @return 包含 {@link RecordMetadata} 的 CompletableFuture
     */
    CompletableFuture<RecordMetadata> publish(T data);

    /**
     * 批量发送消息到默认主题
     *
     * @param dataList 消息体列表
     * @return 包含每条消息 {@link RecordMetadata} 的 CompletableFuture 列表
     */
    List<CompletableFuture<RecordMetadata>> publishBatch(List<T> dataList);

    /**
     * 发送带 Key 的单条消息到默认主题
     * <p>
     * Key 用于分区路由，相同 Key 的消息会被发送到同一分区。
     *
     * @param key  消息 Key
     * @param data 消息体
     * @return 包含 {@link RecordMetadata} 的 CompletableFuture
     */
    CompletableFuture<RecordMetadata> publishWithKey(String key, T data);

    /**
     * 发送单条消息到指定主题
     *
     * @param topic 目标主题名
     * @param data  消息体
     * @return 包含 {@link RecordMetadata} 的 CompletableFuture
     */
    CompletableFuture<RecordMetadata> publishToTopic(String topic, T data);
}
