package com.microservice.framework.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KafkaProperties 绑定和默认值测试
 * <p>
 * 验证各嵌套配置组的默认值符合设计规格，
 * 并验证属性绑定后值能正确覆盖默认值。
 *
 * @author Andy Yang
 */
class KafkaPropertiesTest {

    @Test
    @DisplayName("默认生产者配置应与设计规格一致")
    void defaultProducerPropertiesShouldMatchSpecification() {
        KafkaProperties properties = new KafkaProperties();
        KafkaProperties.ProducerProperties producer = properties.getProducer();

        assertThat(producer.getAcks()).isEqualTo("all");
        assertThat(producer.getRetries()).isEqualTo(3);
        assertThat(producer.getBatchSize()).isEqualTo(16384);
        assertThat(producer.getLingerMs()).isEqualTo(0);
    }

    @Test
    @DisplayName("默认消费者配置应与设计规格一致")
    void defaultConsumerPropertiesShouldMatchSpecification() {
        KafkaProperties properties = new KafkaProperties();
        KafkaProperties.ConsumerProperties consumer = properties.getConsumer();

        assertThat(consumer.getAutoCommit()).isFalse();
        assertThat(consumer.getConcurrency()).isEqualTo(3);
        assertThat(consumer.getAutoCommitIntervalMs()).isEqualTo(1000);
    }

    @Test
    @DisplayName("默认死信配置应与设计规格一致")
    void defaultDeadLetterPropertiesShouldMatchSpecification() {
        KafkaProperties properties = new KafkaProperties();
        KafkaProperties.DeadLetterProperties deadLetter = properties.getDeadLetter();

        assertThat(deadLetter.getEnabled()).isTrue();
        assertThat(deadLetter.getMaxRetries()).isEqualTo(3);
        assertThat(deadLetter.getTopicSuffix()).isEqualTo(".DLT");
    }

    @Test
    @DisplayName("默认幂等配置应与设计规格一致")
    void defaultIdempotencyPropertiesShouldMatchSpecification() {
        KafkaProperties properties = new KafkaProperties();
        KafkaProperties.IdempotencyProperties idempotency = properties.getIdempotency();

        assertThat(idempotency.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("自定义属性值应能覆盖默认值")
    void customPropertyValuesShouldOverrideDefaults() {
        KafkaProperties properties = new KafkaProperties();

        properties.getProducer().setAcks("1");
        properties.getProducer().setRetries(5);
        properties.getProducer().setBatchSize(32768);
        properties.getProducer().setLingerMs(10);

        assertThat(properties.getProducer().getAcks()).isEqualTo("1");
        assertThat(properties.getProducer().getRetries()).isEqualTo(5);
        assertThat(properties.getProducer().getBatchSize()).isEqualTo(32768);
        assertThat(properties.getProducer().getLingerMs()).isEqualTo(10);

        properties.getConsumer().setAutoCommit(true);
        properties.getConsumer().setConcurrency(6);
        assertThat(properties.getConsumer().getAutoCommit()).isTrue();
        assertThat(properties.getConsumer().getConcurrency()).isEqualTo(6);

        properties.getDeadLetter().setEnabled(false);
        properties.getDeadLetter().setMaxRetries(5);
        properties.getDeadLetter().setTopicSuffix(".DLQ");
        assertThat(properties.getDeadLetter().getEnabled()).isFalse();
        assertThat(properties.getDeadLetter().getMaxRetries()).isEqualTo(5);
        assertThat(properties.getDeadLetter().getTopicSuffix()).isEqualTo(".DLQ");

        properties.getIdempotency().setEnabled(false);
        assertThat(properties.getIdempotency().getEnabled()).isFalse();
    }
}
