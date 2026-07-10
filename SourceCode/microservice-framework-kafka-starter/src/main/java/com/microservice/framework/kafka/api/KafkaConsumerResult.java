package com.microservice.framework.kafka.api;

/**
 * 单条 Kafka 消费结果。
 *
 * @param topic     主题
 * @param partition 分区
 * @param offset    位点
 * @param key       消息 key
 * @param value     消息 value
 * @param timestamp 消息时间戳
 */
public record KafkaConsumerResult(
        String topic,
        int partition,
        long offset,
        Object key,
        Object value,
        long timestamp) {
}
