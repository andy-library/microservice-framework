package com.microservice.demo.controller;

import com.microservice.framework.elasticsearch.api.ElasticsearchOperations;
import com.microservice.framework.elasticsearch.api.IndexManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * elasticsearch-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/elasticsearch")
public class ElasticsearchTestController extends ElasticsearchDemoController {
    public ElasticsearchTestController(ElasticsearchOperations operations, IndexManager indexManager) {
        super(operations, indexManager);
    }
}
