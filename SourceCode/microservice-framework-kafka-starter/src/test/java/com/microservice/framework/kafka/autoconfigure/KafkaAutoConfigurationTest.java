package com.microservice.framework.kafka.autoconfigure;

import com.microservice.framework.kafka.api.DeadLetterHandler;
import com.microservice.framework.kafka.api.IdempotencyFilter;
import com.microservice.framework.kafka.api.KafkaConsumerBuilder;
import com.microservice.framework.kafka.api.KafkaPublisher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("默认重试处理器在没有 KafkaTemplate 时必须失败关闭")
    void retryMustFailClosedWhenNoKafkaTemplateIsAvailable() {
        contextRunner.run(context -> {
            DeadLetterHandler<Object, Object> handler = context.getBean(DeadLetterHandler.class);

            assertThat(handler.retry(new ConsumerRecord<>("orders", 0, 1L, "key", "value"), 0)).isFalse();
        });
    }

    @Test
    @DisplayName("默认重试处理器应确认重新发布原始消息后才报告成功")
    void retryShouldRepublishOriginalRecordBeforeReportingSuccess() {
        contextRunner.withUserConfiguration(KafkaTemplateConfiguration.class)
                .run(context -> {
                    KafkaTemplate<Object, Object> kafkaTemplate = context.getBean(KafkaTemplate.class);
                    when(kafkaTemplate.send(eq("orders"), eq("key"), eq("value")))
                            .thenReturn(CompletableFuture.completedFuture(null));
                    DeadLetterHandler<Object, Object> handler = context.getBean(DeadLetterHandler.class);

                    assertThat(handler.retry(new ConsumerRecord<>("orders", 0, 1L, "key", "value"), 0)).isTrue();

                    verify(kafkaTemplate).send("orders", "key", "value");
                });
    }

    @Test
    @DisplayName("生产环境必须提供分布式 IdempotencyFilter")
    void productionShouldRejectDefaultLocalIdempotencyFilter() {
        contextRunner.withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("默认幂等过滤器应有界淘汰最早的本地记录")
    void defaultIdempotencyFilterShouldBeBounded() {
        contextRunner.withPropertyValues("framework.kafka.idempotency.max-entries=2")
                .run(context -> {
                    IdempotencyFilter<Object, Object> filter = context.getBean(IdempotencyFilter.class);
                    ConsumerRecord<Object, Object> first = new ConsumerRecord<>("orders", 0, 1L, "key-1", "value");
                    ConsumerRecord<Object, Object> second = new ConsumerRecord<>("orders", 0, 2L, "key-2", "value");
                    ConsumerRecord<Object, Object> third = new ConsumerRecord<>("orders", 0, 3L, "key-3", "value");

                    filter.markProcessed(first);
                    filter.markProcessed(second);
                    filter.markProcessed(third);

                    assertThat(filter.isDuplicate(first)).isFalse();
                    assertThat(filter.isDuplicate(second)).isTrue();
                    assertThat(filter.isDuplicate(third)).isTrue();
                });
    }

    @Test
    @DisplayName("框架生产者设置应写入实际 Kafka ProducerFactory")
    void frameworkProducerSettingsShouldBeAppliedToProducerFactory() {
        contextRunner.withUserConfiguration(ProducerFactoryConfiguration.class)
                .withPropertyValues(
                        "framework.kafka.producer.acks=1",
                        "framework.kafka.producer.retries=7",
                        "framework.kafka.producer.batch-size=8192",
                        "framework.kafka.producer.linger-ms=12")
                .run(context -> {
                    DefaultKafkaProducerFactory<?, ?> factory = context.getBean(DefaultKafkaProducerFactory.class);

                    assertThat(factory.getConfigurationProperties())
                            .containsEntry(ProducerConfig.ACKS_CONFIG, "1")
                            .containsEntry(ProducerConfig.RETRIES_CONFIG, 7)
                            .containsEntry(ProducerConfig.BATCH_SIZE_CONFIG, 8192)
                            .containsEntry(ProducerConfig.LINGER_MS_CONFIG, 12);
                });
    }

    @Test
    @DisplayName("框架生产者设置与现有 ProducerFactory 冲突时应快速失败")
    void conflictingProducerFactorySettingsShouldFailFast() {
        contextRunner.withUserConfiguration(ConflictingProducerFactoryConfiguration.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    static class KafkaTemplateConfiguration {

        @Bean
        KafkaTemplate<Object, Object> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProducerFactoryConfiguration {

        @Bean
        DefaultKafkaProducerFactory<Object, Object> producerFactory() {
            return new DefaultKafkaProducerFactory<>(new HashMap<>());
        }

        @Bean
        KafkaTemplate<Object, Object> kafkaTemplate(DefaultKafkaProducerFactory<Object, Object> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ConflictingProducerFactoryConfiguration {

        @Bean
        DefaultKafkaProducerFactory<Object, Object> producerFactory() {
            return new DefaultKafkaProducerFactory<>(new HashMap<>(Map.of(ProducerConfig.ACKS_CONFIG, "1")));
        }

        @Bean
        KafkaTemplate<Object, Object> kafkaTemplate(DefaultKafkaProducerFactory<Object, Object> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }
    }
}
