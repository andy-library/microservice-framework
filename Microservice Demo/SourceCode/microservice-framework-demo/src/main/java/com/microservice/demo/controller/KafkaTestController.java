package com.microservice.demo.controller;

import com.microservice.framework.kafka.api.KafkaConsumerBuilder;
import com.microservice.framework.kafka.api.KafkaMessageConsumer;
import com.microservice.framework.kafka.api.KafkaPublisher;
import com.microservice.framework.database.api.OutboxPublisher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * kafka-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/kafka")
public class KafkaTestController extends KafkaDemoController {
    public KafkaTestController(KafkaPublisher<Object> publisher, KafkaConsumerBuilder builder,
                               KafkaMessageConsumer consumer, OutboxPublisher outboxPublisher) {
        super(publisher, builder, consumer, outboxPublisher);
    }
}
