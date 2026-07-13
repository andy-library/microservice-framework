package com.microservice.framework.kafka;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Kafka Starter 配置属性
 * <p>
 * 聚合生产者、消费者、死信和幂等配置组，
 * 所有属性前缀为 {@code framework.kafka}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.kafka")
public class KafkaProperties {

    /**
     * 生产者配置
     */
    @NestedConfigurationProperty
    private ProducerProperties producer = new ProducerProperties();

    /**
     * 消费者配置
     */
    @NestedConfigurationProperty
    private ConsumerProperties consumer = new ConsumerProperties();

    /**
     * 死信配置
     */
    @NestedConfigurationProperty
    private DeadLetterProperties deadLetter = new DeadLetterProperties();

    /**
     * 幂等配置
     */
    @NestedConfigurationProperty
    private IdempotencyProperties idempotency = new IdempotencyProperties();

    // Getters and Setters

    public ProducerProperties getProducer() {
        return producer;
    }

    public void setProducer(ProducerProperties producer) {
        this.producer = producer;
    }

    public ConsumerProperties getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerProperties consumer) {
        this.consumer = consumer;
    }

    public DeadLetterProperties getDeadLetter() {
        return deadLetter;
    }

    public void setDeadLetter(DeadLetterProperties deadLetter) {
        this.deadLetter = deadLetter;
    }

    public IdempotencyProperties getIdempotency() {
        return idempotency;
    }

    public void setIdempotency(IdempotencyProperties idempotency) {
        this.idempotency = idempotency;
    }

    /**
     * 生产者配置
     */
    public static class ProducerProperties {

        /**
         * 确认级别，默认 all
         * <p>
         * all 表示等待所有副本确认，提供最强持久性保证。
         */
        @NotBlank
        private String acks = "all";

        /**
         * 重试次数，默认 3
         * <p>
         * 发送失败时自动重试，0 表示不重试。
         */
        @Min(0)
        private Integer retries = 3;

        /**
         * 批量发送大小（字节），默认 16384
         * <p>
         * 当多条消息同时发送时，批量打包可提升吞吐量。
         */
        @Min(0)
        private Integer batchSize = 16384;

        /**
         * 批量延迟等待时间（毫秒），默认 0
         * <p>
         * 等待此时间后再发送，以凑够 batchSize。0 表示立即发送。
         */
        @Min(0)
        private Integer lingerMs = 0;

        // Getters and Setters

        public String getAcks() {
            return acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public Integer getRetries() {
            return retries;
        }

        public void setRetries(Integer retries) {
            this.retries = retries;
        }

        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }

        public Integer getLingerMs() {
            return lingerMs;
        }

        public void setLingerMs(Integer lingerMs) {
            this.lingerMs = lingerMs;
        }
    }

    /**
     * 消费者配置
     */
    public static class ConsumerProperties {

        /**
         * 是否启用自动提交，默认 false
         * <p>
         * 生产环境推荐手动提交，以保证消费语义的准确性。
         */
        private Boolean autoCommit = false;

        /**
         * 消费者并发数，默认 3
         * <p>
         * 每个主题分区对应的并发消费者线程数。
         */
        @Min(1)
        private Integer concurrency = 3;

        /**
         * 自动提交间隔（毫秒），默认 1000
         * <p>
         * 仅在 autoCommit=true 时生效。
         */
        @Min(100)
        private Integer autoCommitIntervalMs = 1000;

        /** Maximum time for an explicit single-record poll. */
        @Min(100)
        private Long pollTimeoutMs = 10000L;

        // Getters and Setters

        public Boolean getAutoCommit() {
            return autoCommit;
        }

        public void setAutoCommit(Boolean autoCommit) {
            this.autoCommit = autoCommit;
        }

        public Integer getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(Integer concurrency) {
            this.concurrency = concurrency;
        }

        public Integer getAutoCommitIntervalMs() {
            return autoCommitIntervalMs;
        }

        public void setAutoCommitIntervalMs(Integer autoCommitIntervalMs) {
            this.autoCommitIntervalMs = autoCommitIntervalMs;
        }

        public Long getPollTimeoutMs() {
            return pollTimeoutMs;
        }

        public void setPollTimeoutMs(Long pollTimeoutMs) {
            this.pollTimeoutMs = pollTimeoutMs;
        }
    }

    /**
     * 死信配置
     */
    public static class DeadLetterProperties {

        /**
         * 是否启用死信队列，默认 true
         */
        private Boolean enabled = true;

        /**
         * 最大重试次数，默认 3
         * <p>
         * 消费失败超过此次数后，消息将被转发到死信主题。
         */
        @Min(1)
        private Integer maxRetries = 3;

        /**
         * 死信主题后缀，默认 ".DLT"
         * <p>
         * 原始主题名 + 后缀即为死信主题名。
         */
        @NotBlank
        private String topicSuffix = ".DLT";

        // Getters and Setters

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public String getTopicSuffix() {
            return topicSuffix;
        }

        public void setTopicSuffix(String topicSuffix) {
            this.topicSuffix = topicSuffix;
        }
    }

    /**
     * 幂等配置
     */
    public static class IdempotencyProperties {

        /**
         * 是否启用幂等消费过滤，默认 true
         * <p>
         * 启用后，重复消费的消息将被自动跳过。
         */
        private Boolean enabled = true;

        /**
         * 默认本地幂等缓存最大记录数。
         * <p>
         * 仅用于非生产环境的默认实现，生产环境应提供 Redis 或数据库等分布式实现。
         */
        @Min(1)
        private Integer maxEntries = 10000;

        // Getters and Setters

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getMaxEntries() {
            return maxEntries;
        }

        public void setMaxEntries(Integer maxEntries) {
            this.maxEntries = maxEntries;
        }
    }
}
