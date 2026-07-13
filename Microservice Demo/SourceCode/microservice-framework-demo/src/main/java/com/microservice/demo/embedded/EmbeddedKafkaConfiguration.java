package com.microservice.demo.embedded;

import com.microservice.framework.kafka.api.KafkaPublisher;
import com.microservice.framework.kafka.api.KafkaMessageConsumer;
import com.microservice.framework.kafka.api.KafkaConsumerResult;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Optional;

/**
 * Embedded Kafka Configuration
 *
 * Provides an in-memory KafkaPublisher bean that overrides the starter's default
 * implementation via {@code @ConditionalOnMissingBean}. Activates only when
 * {@code framework.kafka.provider=embedded} is set.
 *
 * <p><b>RecordMetadata handling:</b> Since {@code org.apache.kafka.clients.producer.RecordMetadata}
 * is not available on the compile classpath (spring-kafka is excluded at compile scope
 * in the demo project), this implementation uses raw return types for methods that
 * would normally return {@code CompletableFuture<RecordMetadata>}. The existing
 * {@code KafkaDemoController} already handles the return type via reflection and
 * gracefully accepts null metadata objects.</p>
 *
 * <p>Messages are stored in a ConcurrentLinkedQueue for retrieval and verification
 * during integration testing.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "framework.kafka", name = "provider", havingValue = "embedded")
public class EmbeddedKafkaConfiguration {

    @Bean
    public InMemoryKafkaPublisher<Object> inMemoryKafkaPublisher() {
        return new InMemoryKafkaPublisher<>();
    }

    @Bean
    public KafkaMessageConsumer inMemoryKafkaMessageConsumer(InMemoryKafkaPublisher<Object> publisher) {
        return new KafkaMessageConsumer() {
            @Override
            public Optional<KafkaConsumerResult> pollOne(String topic, String groupId) {
                return publisher.poll(topic).map(message -> new KafkaConsumerResult(
                        message.topic, 0, message.timestampMs, message.key, message.data, message.timestampMs));
            }

            @Override
            public CompletableFuture<RecordMetadata> publishToDeadLetter(
                    String topic, Object key, Object value, String reason) {
                return publisher.publishToTopic(deadLetterTopic(topic), value);
            }

            @Override
            public String deadLetterTopic(String topic) { return topic + ".DLT"; }
        };
    }

    /**
     * ConcurrentLinkedQueue-backed in-memory KafkaPublisher.
     *
     * <p>Stores published messages in an internal queue for test verification.
     * Returns {@code CompletableFuture.completedFuture(null)} for all publish methods,
     * since {@code RecordMetadata} is not on the compile classpath. The
     * {@code KafkaDemoController} handles null results gracefully via reflection.</p>
     *
     * <p>Raw types are used in method signatures to avoid requiring
     * {@code org.apache.kafka.clients.producer.RecordMetadata} at compile time.</p>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static class InMemoryKafkaPublisher<T> implements KafkaPublisher<T> {

        private final ConcurrentLinkedQueue<PublishedMessage<T>> messageQueue = new ConcurrentLinkedQueue<>();

        /**
         * Record of a published message, stored for test verification.
         */
        static class PublishedMessage<T> {
            final T data;
            final String key;
            final String topic;
            final long timestampMs;

            PublishedMessage(T data, String key, String topic) {
                this.data = data;
                this.key = key;
                this.topic = topic;
                this.timestampMs = System.currentTimeMillis();
            }
        }

        /**
         * Returns the list of all published messages (for test verification).
         */
        public List<PublishedMessage<T>> getPublishedMessages() {
            return new ArrayList<>(messageQueue);
        }

        /**
         * Returns the count of published messages.
         */
        public int getPublishedMessageCount() {
            return messageQueue.size();
        }

        /**
         * Clears all stored messages.
         */
        public void clearMessages() {
            messageQueue.clear();
        }

        Optional<PublishedMessage<T>> poll(String topic) {
            for (PublishedMessage<T> message : messageQueue) {
                if (topic.equals(message.topic) && messageQueue.remove(message)) {
                    return Optional.of(message);
                }
            }
            return Optional.empty();
        }

        // ======================================================================
        // KafkaPublisher interface implementation
        // ======================================================================
        // Note: Raw return types are used because RecordMetadata is not on the
        // compile classpath. After type erasure, CompletableFuture<RecordMetadata>
        // becomes CompletableFuture, so raw type overrides are valid Java.

        @Override
        public CompletableFuture publish(T data) {
            messageQueue.add(new PublishedMessage<>(data, null, "default-topic"));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public List publishBatch(List dataList) {
            List futures = new ArrayList();
            if (dataList != null) {
                for (T data : (List<T>) dataList) {
                    messageQueue.add(new PublishedMessage<>(data, null, "default-topic"));
                    futures.add(CompletableFuture.completedFuture(null));
                }
            }
            return futures;
        }

        @Override
        public CompletableFuture publishWithKey(String key, T data) {
            messageQueue.add(new PublishedMessage<>(data, key, "default-topic"));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture publishToTopic(String topic, T data) {
            messageQueue.add(new PublishedMessage<>(data, null, topic));
            return CompletableFuture.completedFuture(null);
        }
    }
}
