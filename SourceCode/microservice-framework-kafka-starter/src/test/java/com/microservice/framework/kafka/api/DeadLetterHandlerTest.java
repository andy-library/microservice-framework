package com.microservice.framework.kafka.api;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeadLetterHandler 契约测试
 * <p>
 * 使用 stub 实现验证接口方法签名和语义，
 * 不依赖真实的 Kafka 集群。
 *
 * @author Andy Yang
 */
class DeadLetterHandlerTest {

    private final DeadLetterHandler<String, String> handler = new StubDeadLetterHandler();

    @Test
    @DisplayName("handle 应正确接收死信消息")
    void handleShouldReceiveDeadLetterRecord() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 100L, "key-1", "value-1");

        // Stub 实现记录了最后处理的消息
        handler.handle(record);
        assertThat(((StubDeadLetterHandler) handler).getLastHandledRecord()).isEqualTo(record);
    }

    @Test
    @DisplayName("retry 在重试次数未超限时应返回 true")
    void retryShouldReturnTrueWhenRetryCountBelowMax() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 100L, "key-1", "value-1");

        assertThat(handler.retry(record, 0)).isTrue();
        assertThat(handler.retry(record, 1)).isTrue();
        assertThat(handler.retry(record, 2)).isTrue();
    }

    @Test
    @DisplayName("retry 在重试次数超限时应返回 false")
    void retryShouldReturnFalseWhenRetryCountExceedsMax() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 100L, "key-1", "value-1");

        assertThat(handler.retry(record, 3)).isFalse();
        assertThat(handler.retry(record, 4)).isFalse();
    }

    @Test
    @DisplayName("getMaxRetries 应返回配置的最大重试次数")
    void getMaxRetriesShouldReturnConfiguredMaxRetries() {
        assertThat(handler.getMaxRetries()).isEqualTo(3);
    }

    // ========================================================================
    // Stub 实现
    // ========================================================================

    static class StubDeadLetterHandler implements DeadLetterHandler<String, String> {

        private ConsumerRecord<String, String> lastHandledRecord;

        @Override
        public void handle(ConsumerRecord<String, String> record) {
            this.lastHandledRecord = record;
        }

        @Override
        public boolean retry(ConsumerRecord<String, String> record, int retryCount) {
            return retryCount < getMaxRetries();
        }

        @Override
        public int getMaxRetries() {
            return 3;
        }

        ConsumerRecord<String, String> getLastHandledRecord() {
            return lastHandledRecord;
        }
    }
}
