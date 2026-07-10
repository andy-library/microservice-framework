package com.microservice.framework.kafka.api;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IdempotencyFilter 契约测试
 * <p>
 * 使用 stub 实现验证接口方法签名和幂等语义，
 * 不依赖真实的 Kafka 集群或外部存储。
 *
 * @author Andy Yang
 */
class IdempotencyFilterTest {

    private final IdempotencyFilter<String, String> filter = new StubIdempotencyFilter();

    @Test
    @DisplayName("未处理消息 isDuplicate 应返回 false")
    void unprocessedMessageShouldNotBeDuplicate() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 100L, "key-1", "value-1");

        assertThat(filter.isDuplicate(record)).isFalse();
    }

    @Test
    @DisplayName("markProcessed 后 isDuplicate 应返回 true")
    void processedMessageShouldBeDuplicateAfterMarking() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 100L, "key-1", "value-1");

        assertThat(filter.isDuplicate(record)).isFalse();
        filter.markProcessed(record);
        assertThat(filter.isDuplicate(record)).isTrue();
    }

    @Test
    @DisplayName("不同 offset 的消息应独立判断")
    void differentOffsetsShouldBeIndependent() {
        ConsumerRecord<String, String> record1 = new ConsumerRecord<>("orders", 0, 100L, "key-1", "value-1");
        ConsumerRecord<String, String> record2 = new ConsumerRecord<>("orders", 0, 101L, "key-2", "value-2");

        filter.markProcessed(record1);

        assertThat(filter.isDuplicate(record1)).isTrue();
        assertThat(filter.isDuplicate(record2)).isFalse();
    }

    @Test
    @DisplayName("同一主题不同分区的消息应独立判断")
    void differentPartitionsShouldBeIndependent() {
        ConsumerRecord<String, String> record1 = new ConsumerRecord<>("orders", 0, 100L, "key-1", "value-1");
        ConsumerRecord<String, String> record2 = new ConsumerRecord<>("orders", 1, 100L, "key-2", "value-2");

        filter.markProcessed(record1);

        assertThat(filter.isDuplicate(record1)).isTrue();
        assertThat(filter.isDuplicate(record2)).isFalse();
    }

    // ========================================================================
    // Stub 实现
    // ========================================================================

    static class StubIdempotencyFilter implements IdempotencyFilter<String, String> {

        private final java.util.Set<String> processedKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

        @Override
        public boolean isDuplicate(ConsumerRecord<String, String> record) {
            return processedKeys.contains(buildKey(record));
        }

        @Override
        public void markProcessed(ConsumerRecord<String, String> record) {
            processedKeys.add(buildKey(record));
        }

        private String buildKey(ConsumerRecord<String, String> record) {
            return record.topic() + "-" + record.partition() + "-" + record.offset();
        }
    }
}
