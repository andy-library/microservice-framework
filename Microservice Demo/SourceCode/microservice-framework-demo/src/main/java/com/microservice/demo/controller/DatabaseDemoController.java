package com.microservice.demo.controller;

import com.microservice.demo.entity.DemoOrderEntity;
import com.microservice.demo.repository.DemoOrderRepository;
import com.microservice.framework.database.api.OutboxPublisher;
import com.microservice.framework.database.api.ReadWriteRoutingContext;
import com.microservice.framework.database.api.TransactionTemplateFacade;
import com.microservice.framework.json.api.JsonCodec;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * database-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/database")
public class DatabaseDemoController {

    private final DemoOrderRepository repository;
    private final TransactionTemplateFacade transactions;
    private final OutboxPublisher outbox;
    private final JsonCodec jsonCodec;
    private final ReadWriteRoutingContext routingContext;

    public DatabaseDemoController(DemoOrderRepository repository, TransactionTemplateFacade transactions,
                                  OutboxPublisher outbox, JsonCodec jsonCodec,
                                  ReadWriteRoutingContext routingContext) {
        this.repository = repository;
        this.transactions = transactions;
        this.outbox = outbox;
        this.jsonCodec = jsonCodec;
        this.routingContext = routingContext;
    }

    @PostMapping("/order")
    public ApiResponse<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        DemoOrderEntity entity = new DemoOrderEntity();
        entity.setOrderNo(String.valueOf(body.getOrDefault("orderNo", UUID.randomUUID().toString())));
        entity.setAmount(new BigDecimal(String.valueOf(body.getOrDefault("amount", "0"))));
        entity.setStatus(String.valueOf(body.getOrDefault("status", "CREATED")));
        entity = repository.save(entity);
        return ApiResponse.success(Map.of("operation", "create", "id", entity.getId(), "orderNo", entity.getOrderNo()));
    }

    @GetMapping("/order/{id}")
    public ApiResponse<Map<String, Object>> getOrder(@PathVariable Long id) {
        DemoOrderEntity entity = repository.findById(id).orElseThrow();
        return ApiResponse.success(Map.of("id", entity.getId(), "orderNo", entity.getOrderNo(), "status", entity.getStatus()));
    }

    @PostMapping("/transaction")
    public ApiResponse<Map<String, Object>> transaction() {
        long before = repository.count();
        try {
            transactions.execute(() -> {
                DemoOrderEntity transientOrder = new DemoOrderEntity();
                transientOrder.setOrderNo("ROLLBACK-" + UUID.randomUUID());
                transientOrder.setAmount(BigDecimal.ZERO);
                transientOrder.setStatus("ROLLBACK_TEST");
                repository.saveAndFlush(transientOrder);
                throw new IllegalStateException("intentional rollback verification");
            });
        } catch (RuntimeException expected) {
            // The endpoint verifies the transaction facade by observing persisted state.
        }
        boolean rolledBack = repository.count() == before;
        return ApiResponse.success(Map.of("verificationPassed", rolledBack, "rolledBack", rolledBack,
                "method", transactions.getClass().getSimpleName()));
    }

    @GetMapping("/routing/current")
    public ApiResponse<Map<String, Object>> routing() {
        String route = routingContext.currentRoute().name();
        return ApiResponse.success(Map.of("routingHint", route, "isPrimary", "PRIMARY".equals(route)));
    }

    @PostMapping("/outbox/publish")
    public ApiResponse<Map<String, Object>> publishOutbox(@RequestBody Map<String, Object> body) {
        String aggregateId = String.valueOf(body.getOrDefault("aggregateId", UUID.randomUUID().toString()));
        outbox.publish(String.valueOf(body.getOrDefault("aggregateType", "Demo")), aggregateId,
                String.valueOf(body.getOrDefault("eventType", "CREATED")), jsonCodec.serialize(body));
        String eventId = outbox.findUnpublished().stream()
                .filter(event -> aggregateId.equals(event.getAggregateId()))
                .map(OutboxPublisher.OutboxEvent::getEventId)
                .findFirst().orElseThrow();
        return ApiResponse.success(Map.of("operation", "publish", "aggregateId", aggregateId,
                "eventId", eventId, "stored", true));
    }

    @GetMapping("/outbox/unpublished")
    public ApiResponse<Map<String, Object>> unpublished() {
        var events = outbox.findUnpublished();
        return ApiResponse.success(Map.of("count", events.size(), "events", events));
    }

    @PostMapping("/outbox/{eventId}/mark-published")
    public ApiResponse<Map<String, Object>> markPublished(@PathVariable String eventId) {
        outbox.markPublished(eventId);
        return ApiResponse.success(Map.of("operation", "markPublished", "eventId", eventId, "published", true));
    }
}
