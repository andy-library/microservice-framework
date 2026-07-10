package com.microservice.framework.kafka.autoconfigure;

import com.microservice.framework.kafka.api.DeadLetterHandler;
import com.microservice.framework.kafka.api.IdempotencyFilter;
import com.microservice.framework.kafka.api.KafkaConsumerBuilder;
import com.microservice.framework.kafka.api.KafkaPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka Starter 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证自动配置的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 * <p>
 * 注意：KafkaPublisher 依赖 KafkaTemplate，在测试环境中需通过
 * Spring Boot 的 KafkaAutoConfiguration 创建。其他 Bean（KafkaConsumerBuilder、
 * DeadLetterHandler、IdempotencyFilter）不依赖 KafkaTemplate。
 *
 * @author Andy Yang
 */
class KafkaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    KafkaAutoConfiguration.class));

    @Test
    @DisplayName("默认配置应激活 KafkaConsumerBuilder、DeadLetterHandler 和 IdempotencyFilter")
    void defaultConfigurationShouldActivateCoreBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("kafkaConsumerBuilder");
            assertThat(context).hasBean("deadLetterHandler");
            assertThat(context).hasBean("idempotencyFilter");
        });
    }

    @Test
    @DisplayName("禁用 Kafka Starter 后所有 Bean 不应存在")
    void disablingKafkaShouldRemoveAllBeans() {
        contextRunner.withPropertyValues("framework.kafka.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(KafkaPublisher.class);
                    assertThat(context).doesNotHaveBean(KafkaConsumerBuilder.class);
                    assertThat(context).doesNotHaveBean(DeadLetterHandler.class);
                    assertThat(context).doesNotHaveBean(IdempotencyFilter.class);
                });
    }

    @Test
    @DisplayName("禁用死信功能后 DeadLetterHandler Bean 不应存在")
    void disablingDeadLetterShouldRemoveDeadLetterHandler() {
        contextRunner.withPropertyValues("framework.kafka.dead-letter.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(DeadLetterHandler.class);
                    assertThat(context).hasBean("kafkaConsumerBuilder");
                    assertThat(context).hasBean("idempotencyFilter");
                });
    }

    @Test
    @DisplayName("禁用幂等功能后 IdempotencyFilter Bean 不应存在")
    void disablingIdempotencyShouldRemoveIdempotencyFilter() {
        contextRunner.withPropertyValues("framework.kafka.idempotency.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(IdempotencyFilter.class);
                    assertThat(context).hasBean("kafkaConsumerBuilder");
                    assertThat(context).hasBean("deadLetterHandler");
                });
    }

    @Test
    @DisplayName("KafkaConsumerBuilder 应使用默认消费者配置属性")
    void kafkaConsumerBuilderShouldUseDefaultConsumerProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("kafkaConsumerBuilder");
            KafkaConsumerBuilder builder = context.getBean(KafkaConsumerBuilder.class);
            java.util.Map<String, Object> config = builder
                    .forTopics("test-topic")
                    .withGroupId("test-group")
                    .build();
            assertThat(config.get("enable.auto.commit")).isEqualTo(false);
            assertThat(config.get("auto.commit.interval.ms")).isEqualTo(1000);
            assertThat(config.get("group.id")).isEqualTo("test-group");
            assertThat(config.get("auto.offset.reset")).isEqualTo("latest");
        });
    }

    @Test
    @DisplayName("自定义消费者属性应覆盖默认值")
    void customConsumerPropertiesShouldOverrideDefaults() {
        contextRunner.withPropertyValues(
                "framework.kafka.consumer.auto-commit=true",
                "framework.kafka.consumer.concurrency=5",
                "framework.kafka.consumer.auto-commit-interval-ms=2000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    KafkaConsumerBuilder builder = context.getBean(KafkaConsumerBuilder.class);
                    java.util.Map<String, Object> config = builder
                            .forTopics("test-topic")
                            .withGroupId("test-group")
                            .build();
                    assertThat(config.get("enable.auto.commit")).isEqualTo(true);
                    assertThat(config.get("auto.commit.interval.ms")).isEqualTo(2000);
                });
    }

    @Test
    @DisplayName("KafkaPublisher 仅在 KafkaTemplate Bean 存在时激活")
    void kafkaPublisherShouldOnlyActivateWhenKafkaTemplateIsPresent() {
        // Without KafkaTemplate, KafkaPublisher should NOT be created
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(KafkaPublisher.class);
        });
    }
}
