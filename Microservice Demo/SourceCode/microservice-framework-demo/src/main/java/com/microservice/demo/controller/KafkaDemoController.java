package com.microservice.demo.controller;

import com.microservice.framework.kafka.api.KafkaConsumerBuilder;
import com.microservice.framework.kafka.api.KafkaConsumerResult;
import com.microservice.framework.kafka.api.KafkaMessageConsumer;
import com.microservice.framework.kafka.api.KafkaPublisher;
import com.microservice.framework.database.api.OutboxPublisher;
import com.microservice.framework.web.api.ApiResponse;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** kafka-starter public capability endpoints. @author Andy Yang */
@RestController
@RequestMapping("/demo/kafka")
public class KafkaDemoController {
    private final KafkaPublisher<Object> publisher;
    private final KafkaConsumerBuilder consumerBuilder;
    private final KafkaMessageConsumer consumer;
    private final OutboxPublisher outboxPublisher;

    public KafkaDemoController(KafkaPublisher<Object> publisher, KafkaConsumerBuilder consumerBuilder,
                               KafkaMessageConsumer consumer, OutboxPublisher outboxPublisher) {
        this.publisher = publisher;
        this.consumerBuilder = consumerBuilder;
        this.consumer = consumer;
        this.outboxPublisher = outboxPublisher;
    }

    @PostMapping({"/publish", "/send"})
    public ApiResponse<Map<String, Object>> publish(@RequestBody Map<String, Object> body) throws Exception {
        String topic = String.valueOf(body.getOrDefault("topic", "demo-topic"));
        RecordMetadata metadata = publisher.publishToTopic(topic, body.getOrDefault("message", body))
                .get(5, TimeUnit.SECONDS);
        return ApiResponse.success(Map.of("published", true, "topic", topic,
                "partition", metadata == null ? -1 : metadata.partition(),
                "offset", metadata == null ? -1L : metadata.offset(),
                "status", "sent", "recordMetadataClass",
                metadata == null ? "unavailable" : metadata.getClass().getSimpleName()));
    }

    @PostMapping("/send-async")
    public ApiResponse<Map<String, Object>> sendAsync(@RequestBody Map<String, Object> body) {
        String topic = String.valueOf(body.getOrDefault("topic", "demo-topic"));
        publisher.publishToTopic(topic, body.getOrDefault("message", body));
        return ApiResponse.success(Map.of("topic", topic, "submitted", true, "status", "async-sent", "publisherUsed", true));
    }

    @GetMapping("/consumer/status")
    public ApiResponse<Map<String, Object>> consumerStatus() {
        Object configuration = consumerBuilder.build();
        return ApiResponse.success(Map.of("configuration", configuration,
                "consumerBuilderInfo", configuration,
                "kafkaPublisherAvailable", true, "kafkaConsumerBuilderAvailable", true));
    }

    @PostMapping("/consume-one")
    public ApiResponse<Map<String, Object>> consumeOne(@RequestBody Map<String, Object> body) {
        String topic = String.valueOf(body.getOrDefault("topic", "demo-topic"));
        String group = String.valueOf(body.getOrDefault("groupId", "demo-group"));
        Optional<KafkaConsumerResult> result = consumer.pollOne(topic, group);
        Object record = result.<Object>map(value -> value).orElse(Map.of());
        Object value = result.<Object>map(KafkaConsumerResult::value).orElse(Map.of());
        return ApiResponse.success(Map.of("topic", topic, "groupId", group,
                "consumed", result.isPresent(), "found", result.isPresent(),
                "record", record, "value", value));
    }

    @PostMapping("/dlq/publish")
    public ApiResponse<Map<String, Object>> publishDeadLetter(@RequestBody Map<String, Object> body) throws Exception {
        String topic = String.valueOf(body.getOrDefault("topic", "demo-topic"));
        consumer.publishToDeadLetter(topic, body.get("key"), body.get("message"),
                String.valueOf(body.getOrDefault("reason", "demo"))).get(5, TimeUnit.SECONDS);
        return ApiResponse.success(Map.of("topic", topic, "dlqTopic", consumer.deadLetterTopic(topic),
                "published", true, "kafkaAvailable", true));
    }

    @PostMapping("/outbox/publish-pending")
    public ApiResponse<Map<String, Object>> publishPendingOutbox(@RequestBody Map<String, Object> body) {
        String topic = String.valueOf(body.getOrDefault("topic", "demo-topic"));
        int published = 0;
        for (OutboxPublisher.OutboxEvent event : outboxPublisher.findUnpublished()) {
            try {
                publisher.publishToTopic(topic, event.getPayload()).get(5, TimeUnit.SECONDS);
                outboxPublisher.markPublished(event.getEventId());
                published++;
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to publish outbox event " + event.getEventId(), exception);
            }
        }
        return ApiResponse.success(Map.of("operation", "publishPendingOutbox",
                "topic", topic, "published", published));
    }

    @GetMapping("/consumer/manual")
    public ApiResponse<Map<String, Object>> manualConsumer() {
        return ApiResponse.success(Map.of("configuration", consumerBuilder.withProperties(Map.of("enable.auto.commit", false)).build()));
    }

    @GetMapping("/consumer/auto")
    public ApiResponse<Map<String, Object>> autoConsumer() {
        return ApiResponse.success(Map.of("configuration", consumerBuilder.withProperties(Map.of("enable.auto.commit", true)).build()));
    }
}
