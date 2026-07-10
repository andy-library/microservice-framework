package com.microservice.framework.kafka.autoconfigure;

import com.microservice.framework.kafka.KafkaProperties;
import com.microservice.framework.kafka.api.DeadLetterHandler;
import com.microservice.framework.kafka.api.IdempotencyFilter;
import com.microservice.framework.kafka.api.KafkaConsumerBuilder;
import com.microservice.framework.kafka.api.KafkaPublisher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Starter 自动配置
 * <p>
 * 根据 {@code framework.kafka.enabled} 属性决定是否激活，默认启用。
 * 注册以下 Bean：
 * - {@link KafkaPublisher}：消息发布抽象（需要 KafkaTemplate Bean）
 * - {@link KafkaConsumerBuilder}：消费者配置构建器
 * - {@link DeadLetterHandler}：死信消息处理（当 deadLetter.enabled=true）
 * - {@link IdempotencyFilter}：幂等消费过滤（当 idempotency.enabled=true）
 *
 * @author Andy Yang
 */
@AutoConfiguration
@AutoConfigureAfter(org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class)
@EnableConfigurationProperties(KafkaProperties.class)
@ConditionalOnProperty(prefix = "framework.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaAutoConfiguration {

    /**
     * 注册默认 KafkaPublisher Bean
     * <p>
     * 仅当容器中存在 {@link KafkaTemplate} Bean 时激活。
     * 委托给 Spring Kafka 的 {@link KafkaTemplate} 实现消息发送。
     *
     * @param kafkaTemplate Spring Kafka 提供的 KafkaTemplate
     * @return KafkaPublisher 实例
     */
    @Bean
    @ConditionalOnClass(KafkaTemplate.class)
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(KafkaPublisher.class)
    public KafkaPublisher<Object> kafkaPublisher(ObjectProvider<KafkaTemplate<Object, Object>> kafkaTemplateProvider) {
        return new DefaultKafkaPublisher(kafkaTemplateProvider);
    }

    /**
     * 注册默认 KafkaConsumerBuilder Bean
     *
     * @param properties Kafka 配置属性
     * @return KafkaConsumerBuilder 实例
     */
    @Bean
    @ConditionalOnMissingBean(KafkaConsumerBuilder.class)
    public KafkaConsumerBuilder kafkaConsumerBuilder(KafkaProperties properties) {
        return new DefaultKafkaConsumerBuilder(properties);
    }

    /**
     * 注册默认 DeadLetterHandler Bean
     * <p>
     * 仅当 {@code framework.kafka.dead-letter.enabled=true} 时激活。
     *
     * @param properties Kafka 配置属性
     * @return DeadLetterHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean(DeadLetterHandler.class)
    @ConditionalOnProperty(prefix = "framework.kafka.dead-letter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DeadLetterHandler<Object, Object> deadLetterHandler(KafkaProperties properties) {
        return new DefaultDeadLetterHandler(properties);
    }

    /**
     * 注册默认 IdempotencyFilter Bean
     * <p>
     * 仅当 {@code framework.kafka.idempotency.enabled=true} 时激活。
     *
     * @return IdempotencyFilter 实例
     */
    @Bean
    @ConditionalOnMissingBean(IdempotencyFilter.class)
    @ConditionalOnProperty(prefix = "framework.kafka.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IdempotencyFilter<Object, Object> idempotencyFilter() {
        return new DefaultIdempotencyFilter();
    }

    // ========================================================================
    // 默认内部实现
    // ========================================================================

    /**
     * 基于 KafkaTemplate 的默认 KafkaPublisher 实现
     */
    static class DefaultKafkaPublisher implements KafkaPublisher<Object> {

        private final ObjectProvider<KafkaTemplate<Object, Object>> kafkaTemplateProvider;

        DefaultKafkaPublisher(ObjectProvider<KafkaTemplate<Object, Object>> kafkaTemplateProvider) {
            this.kafkaTemplateProvider = kafkaTemplateProvider;
        }

        @Override
        public CompletableFuture<RecordMetadata> publish(Object data) {
            return kafkaTemplate().sendDefault(data)
                    .thenApply(result -> result.getRecordMetadata());
        }

        @Override
        public List<CompletableFuture<RecordMetadata>> publishBatch(List<Object> dataList) {
            return dataList.stream()
                    .map(data -> kafkaTemplate().sendDefault(data)
                            .thenApply(result -> result.getRecordMetadata()))
                    .toList();
        }

        @Override
        public CompletableFuture<RecordMetadata> publishWithKey(String key, Object data) {
            return kafkaTemplate().sendDefault(key, data)
                    .thenApply(result -> result.getRecordMetadata());
        }

        @Override
        public CompletableFuture<RecordMetadata> publishToTopic(String topic, Object data) {
            return kafkaTemplate().send(topic, data)
                    .thenApply(result -> result.getRecordMetadata());
        }

        private KafkaTemplate<Object, Object> kafkaTemplate() {
            KafkaTemplate<Object, Object> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
            if (kafkaTemplate == null) {
                throw new IllegalStateException("KafkaTemplate bean is not available");
            }
            return kafkaTemplate;
        }
    }

    /**
     * 默认 KafkaConsumerBuilder 实现
     */
    static class DefaultKafkaConsumerBuilder implements KafkaConsumerBuilder {

        private final KafkaProperties properties;
        private String[] topics;
        private String groupId;
        private String autoOffsetReset;
        private int concurrency;
        private java.util.Map<String, Object> customProperties;
        private org.apache.kafka.clients.consumer.ConsumerRebalanceListener rebalanceListener;
        private Class<? extends org.apache.kafka.common.serialization.Deserializer<?>> keyDeserializer;
        private Class<? extends org.apache.kafka.common.serialization.Deserializer<?>> valueDeserializer;

        DefaultKafkaConsumerBuilder(KafkaProperties properties) {
            this.properties = properties;
            this.autoOffsetReset = "latest";
            this.concurrency = properties.getConsumer().getConcurrency();
            this.customProperties = new java.util.HashMap<>();
        }

        @Override
        public KafkaConsumerBuilder forTopics(String... topics) {
            this.topics = topics;
            return this;
        }

        @Override
        public KafkaConsumerBuilder withGroupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        @Override
        public KafkaConsumerBuilder withAutoOffsetReset(String strategy) {
            this.autoOffsetReset = strategy;
            return this;
        }

        @Override
        public KafkaConsumerBuilder withConcurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        @Override
        public KafkaConsumerBuilder withKeyDeserializer(Class<? extends org.apache.kafka.common.serialization.Deserializer<?>> deserializer) {
            this.keyDeserializer = deserializer;
            return this;
        }

        @Override
        public KafkaConsumerBuilder withValueDeserializer(Class<? extends org.apache.kafka.common.serialization.Deserializer<?>> deserializer) {
            this.valueDeserializer = deserializer;
            return this;
        }

        @Override
        public KafkaConsumerBuilder withProperties(java.util.Map<String, Object> properties) {
            this.customProperties.putAll(properties);
            return this;
        }

        @Override
        public KafkaConsumerBuilder withRebalanceListener(org.apache.kafka.clients.consumer.ConsumerRebalanceListener listener) {
            this.rebalanceListener = listener;
            return this;
        }

        @Override
        public java.util.Map<String, Object> build() {
            java.util.Map<String, Object> config = new java.util.HashMap<>();
            config.put("auto.offset.reset", autoOffsetReset);
            config.put("enable.auto.commit", properties.getConsumer().getAutoCommit());
            config.put("auto.commit.interval.ms", properties.getConsumer().getAutoCommitIntervalMs());
            if (groupId != null) {
                config.put("group.id", groupId);
            }
            config.putAll(customProperties);
            return config;
        }
    }

    /**
     * 默认 DeadLetterHandler 实现
     */
    static class DefaultDeadLetterHandler implements DeadLetterHandler<Object, Object> {

        private final KafkaProperties properties;

        DefaultDeadLetterHandler(KafkaProperties properties) {
            this.properties = properties;
        }

        @Override
        public void handle(ConsumerRecord<Object, Object> record) {
            // 默认实现仅记录日志；生产环境应覆盖为持久化存储或告警
            System.getLogger("Kafka-DLT").log(System.Logger.Level.WARNING,
                    "Dead letter message received: topic={}, partition={}, offset={}, key={}",
                    record.topic(), record.partition(), record.offset(), record.key());
        }

        @Override
        public boolean retry(ConsumerRecord<Object, Object> record, int retryCount) {
            return retryCount < getMaxRetries();
        }

        @Override
        public int getMaxRetries() {
            return properties.getDeadLetter().getMaxRetries();
        }
    }

    /**
     * 默认 IdempotencyFilter 实现
     * <p>
     * 使用 ConcurrentHashMap 作为本地缓存，适用于单实例部署。
     * 生产环境应覆盖为 Redis 或数据库实现。
     */
    static class DefaultIdempotencyFilter implements IdempotencyFilter<Object, Object> {

        private final java.util.Set<String> processedKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

        @Override
        public boolean isDuplicate(ConsumerRecord<Object, Object> record) {
            String key = buildKey(record);
            return processedKeys.contains(key);
        }

        @Override
        public void markProcessed(ConsumerRecord<Object, Object> record) {
            String key = buildKey(record);
            processedKeys.add(key);
        }

        private String buildKey(ConsumerRecord<Object, Object> record) {
            return record.topic() + "-" + record.partition() + "-" + record.offset();
        }
    }
}
