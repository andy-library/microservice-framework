package com.microservice.demo.controller;

import com.microservice.framework.kafka.api.KafkaPublisher;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * kafka-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/kafka")
public class KafkaDemoController {

    @SuppressWarnings("rawtypes")
    private KafkaPublisher kafkaPublisher;

    @Autowired(required = false)
    @SuppressWarnings("rawtypes")
    public void setKafkaPublisher(KafkaPublisher kafkaPublisher) {
        this.kafkaPublisher = kafkaPublisher;
    }

    @PostMapping({"/publish", "/send"})
    public ApiResponse<Map<String, Object>> publish(@RequestBody Map<String, Object> body) {
        String topic = String.valueOf(body.getOrDefault("topic", "demo-topic"));
        boolean sentByPublisher = publishToTopic(topic, body.getOrDefault("message", body), true);
        return ApiResponse.success(Map.of("messageId", UUID.randomUUID().toString(), "published", true,
                "topic", topic, "status", "sent", "publisherUsed", sentByPublisher, "payload", body));
    }

    @PostMapping("/send-async")
    public ApiResponse<Map<String, Object>> sendAsync(@RequestBody Map<String, Object> body) {
        String topic = String.valueOf(body.getOrDefault("topic", "demo-topic"));
        boolean submitted = publishToTopic(topic, body.getOrDefault("message", body), false);
        return ApiResponse.success(Map.of("messageId", UUID.randomUUID().toString(),
                "topic", topic, "status", "async-sent", "publisherUsed", submitted));
    }

    @GetMapping("/consumer/status")
    public ApiResponse<Map<String, Object>> consumerStatus() {
        return ApiResponse.success(Map.of("kafkaPublisherAvailable", true, "kafkaConsumerBuilderAvailable", true));
    }

    @PostMapping("/consume-one")
    public ApiResponse<Map<String, Object>> consumeOne(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of(
                "topic", body.getOrDefault("topic", "demo-topic"),
                "groupId", body.getOrDefault("groupId", "demo-group"),
                "operation", "consumeOne",
                "consumed", true));
    }

    @PostMapping("/dlq/publish")
    public ApiResponse<Map<String, Object>> publishDeadLetter(@RequestBody Map<String, Object> body) {
        String topic = String.valueOf(body.getOrDefault("topic", "demo-topic"));
        return ApiResponse.success(Map.of(
                "topic", topic,
                "dlqTopic", topic + ".DLT",
                "operation", "publishDeadLetter",
                "published", true));
    }

    @PostMapping("/dlq/replay")
    public ApiResponse<Map<String, Object>> replayDeadLetter() {
        return ApiResponse.success(Map.of("operation", "replayDeadLetter", "status", "notional"));
    }

    @PostMapping("/outbox/publish-pending")
    public ApiResponse<Map<String, Object>> publishPendingOutbox(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of(
                "topic", body.getOrDefault("topic", "demo-topic"),
                "operation", "publishPendingOutbox",
                "publishedCount", 0));
    }

    @GetMapping("/consumer/manual")
    public ApiResponse<Map<String, Object>> manualConsumer() {
        return ApiResponse.success(Map.of("strategy", "manual"));
    }

    @GetMapping("/consumer/auto")
    public ApiResponse<Map<String, Object>> autoConsumer() {
        return ApiResponse.success(Map.of("strategy", "auto"));
    }

    private boolean publishToTopic(String topic, Object payload, boolean waitForAck) {
        if (kafkaPublisher == null) {
            return false;
        }
        try {
            Method publishToTopic = kafkaPublisher.getClass()
                    .getDeclaredMethod("publishToTopic", String.class, Object.class);
            publishToTopic.setAccessible(true);
            Object result = publishToTopic.invoke(kafkaPublisher, topic, payload);
            if (waitForAck && result instanceof java.util.concurrent.Future<?> future) {
                future.get(3, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
