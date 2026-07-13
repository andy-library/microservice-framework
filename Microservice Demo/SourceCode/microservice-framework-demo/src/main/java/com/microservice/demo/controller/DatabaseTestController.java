package com.microservice.demo.controller;

import com.microservice.demo.repository.DemoOrderRepository;
import com.microservice.framework.database.api.OutboxPublisher;
import com.microservice.framework.database.api.TransactionTemplateFacade;
import com.microservice.framework.database.api.ReadWriteRoutingContext;
import com.microservice.framework.json.api.JsonCodec;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * database-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/database")
public class DatabaseTestController extends DatabaseDemoController {
    public DatabaseTestController(DemoOrderRepository repository, TransactionTemplateFacade transactions,
                                  OutboxPublisher outbox, JsonCodec jsonCodec,
                                  ReadWriteRoutingContext routingContext) {
        super(repository, transactions, outbox, jsonCodec, routingContext);
    }
}
