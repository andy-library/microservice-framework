package com.microservice.framework.kafka.api;

import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka 显式消费工具。
 * <p>
 * 该接口不启动后台监听容器，适合应用自行控制的补偿、验收、管理和轻量消费场景。
 */
public interface KafkaMessageConsumer {

    /**
     * 从指定主题拉取一条消息。
     *
     * @param topic   主题名称
     * @param groupId 消费组
     * @return 消费结果，不存在消息时为空
     */
    Optional<KafkaConsumerResult> pollOne(String topic, String groupId);

    /**
     * 将消息投递到死信主题。
     *
     * @param topic  原主题
     * @param key    原消息 key
     * @param value  原消息 value
     * @param reason 死信原因
     * @return Kafka 发送元数据
     */
    CompletableFuture<RecordMetadata> publishToDeadLetter(String topic, Object key, Object value, String reason);

    /**
     * 计算死信主题名称。
     *
     * @param topic 原主题
     * @return 死信主题
     */
    String deadLetterTopic(String topic);
}
