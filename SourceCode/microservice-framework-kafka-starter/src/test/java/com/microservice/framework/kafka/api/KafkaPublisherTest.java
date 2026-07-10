package com.microservice.framework.kafka.api;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * KafkaPublisher 契约测试
 * <p>
 * 使用 mock 实现验证接口方法签名和返回类型，
 * 不依赖真实的 Kafka 集群。
 *
 * @author Andy Yang
 */
class KafkaPublisherTest {

    private final KafkaPublisher<String> publisher = new StubKafkaPublisher();

    @Test
    @DisplayName("publish 应返回 CompletableFuture 包含 RecordMetadata")
    void publishShouldReturnCompletableFutureWithRecordMetadata() {
        CompletableFuture<RecordMetadata> future = publisher.publish("test-message");

        assertThat(future).isCompleted();
        RecordMetadata metadata = future.join();
        assertThat(metadata).isNotNull();
        assertThat(metadata.topic()).isEqualTo("default-topic");
    }

    @Test
    @DisplayName("publishBatch 应返回与输入列表大小相同的 CompletableFuture 列表")
    void publishBatchShouldReturnListOfCompletableFutures() {
        List<String> dataList = List.of("msg-1", "msg-2", "msg-3");
        List<CompletableFuture<RecordMetadata>> futures = publisher.publishBatch(dataList);

        assertThat(futures).hasSize(3);
        assertThat(futures).allSatisfy(f -> assertThat(f).isCompleted());
    }

    @Test
    @DisplayName("publishWithKey 应返回 CompletableFuture 包含 RecordMetadata")
    void publishWithKeyShouldReturnCompletableFutureWithRecordMetadata() {
        CompletableFuture<RecordMetadata> future = publisher.publishWithKey("order-key", "order-data");

        assertThat(future).isCompleted();
        RecordMetadata metadata = future.join();
        assertThat(metadata).isNotNull();
    }

    @Test
    @DisplayName("publishToTopic 应返回 CompletableFuture 包含 RecordMetadata，主题为指定主题")
    void publishToTopicShouldReturnCompletableFutureWithSpecifiedTopic() {
        CompletableFuture<RecordMetadata> future = publisher.publishToTopic("custom-topic", "custom-data");

        assertThat(future).isCompleted();
        RecordMetadata metadata = future.join();
        assertThat(metadata.topic()).isEqualTo("custom-topic");
    }

    @Test
    @DisplayName("publish 空消息不应抛出异常")
    void publishNullDataShouldNotThrow() {
        assertThatCode(() -> publisher.publish(null)).doesNotThrowAnyException();
    }

    // ========================================================================
    // Stub 实现
    // ========================================================================

    static class StubKafkaPublisher implements KafkaPublisher<String> {

        @Override
        public CompletableFuture<RecordMetadata> publish(String data) {
            return completedFuture("default-topic");
        }

        @Override
        public List<CompletableFuture<RecordMetadata>> publishBatch(List<String> dataList) {
            return dataList.stream()
                    .map(data -> completedFuture("default-topic"))
                    .toList();
        }

        @Override
        public CompletableFuture<RecordMetadata> publishWithKey(String key, String data) {
            return completedFuture("default-topic");
        }

        @Override
        public CompletableFuture<RecordMetadata> publishToTopic(String topic, String data) {
            return completedFuture(topic);
        }

        private CompletableFuture<RecordMetadata> completedFuture(String topic) {
            RecordMetadata metadata = new RecordMetadata(
                    new TopicPartition(topic, 0), 0L, 0, 0L, 0, 0);
            return CompletableFuture.completedFuture(metadata);
        }
    }
}
