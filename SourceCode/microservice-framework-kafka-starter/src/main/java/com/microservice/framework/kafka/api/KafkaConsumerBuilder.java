package com.microservice.framework.kafka.api;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * Kafka 消费者配置构建器接口
 * <p>
 * 用于灵活构建消费者配置，支持自定义主题、分组、偏移量策略、
 * 反序列化器和监听器等。
 * <p>
 * 使用示例：
 * <pre>
 * KafkaConsumerBuilder builder = ...;
 * builder.forTopics("order-events", "payment-events")
 *        .withGroupId("order-service")
 *        .withAutoOffsetReset("earliest")
 *        .withConcurrency(4);
 * </pre>
 *
 * @author Andy Yang
 */
public interface KafkaConsumerBuilder {

    /**
     * 指定消费的主题列表
     *
     * @param topics 主题名称列表
     * @return this（链式调用）
     */
    KafkaConsumerBuilder forTopics(String... topics);

    /**
     * 指定消费者组 ID
     *
     * @param groupId 消费者组标识
     * @return this（链式调用）
     */
    KafkaConsumerBuilder withGroupId(String groupId);

    /**
     * 指定偏移量重置策略
     * <p>
     * 有效值：earliest、latest、none
     *
     * @param strategy 偏移量重置策略
     * @return this（链式调用）
     */
    KafkaConsumerBuilder withAutoOffsetReset(String strategy);

    /**
     * 指定消费者并发线程数
     *
     * @param concurrency 并发数
     * @return this（链式调用）
     */
    KafkaConsumerBuilder withConcurrency(int concurrency);

    /**
     * 指定自定义 Key 反序列化器
     *
     * @param deserializer Key 反序列化器类
     * @return this（链式调用）
     */
    KafkaConsumerBuilder withKeyDeserializer(Class<? extends Deserializer<?>> deserializer);

    /**
     * 指定自定义 Value 反序列化器
     *
     * @param deserializer Value 反序列化器类
     * @return this（链式调用）
     */
    KafkaConsumerBuilder withValueDeserializer(Class<? extends Deserializer<?>> deserializer);

    /**
     * 指定自定义消费者属性
     * <p>
     * 用于覆盖默认配置或添加额外属性。
     *
     * @param properties 消费者属性映射
     * @return this（链式调用）
     */
    KafkaConsumerBuilder withProperties(Map<String, Object> properties);

    /**
     * 指定分区重分配监听器
     *
     * @param listener 重分配监听器
     * @return this（链式调用）
     */
    KafkaConsumerBuilder withRebalanceListener(ConsumerRebalanceListener listener);

    /**
     * 构建最终的消费者配置属性映射
     * <p>
     * 将所有配置合并为 Kafka Consumer 所需的属性 Map。
     *
     * @return Kafka Consumer 配置属性
     */
    Map<String, Object> build();
}
