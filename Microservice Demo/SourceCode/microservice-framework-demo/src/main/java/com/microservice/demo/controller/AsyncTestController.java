package com.microservice.demo.controller;

import com.microservice.framework.async.api.AsyncTaskExecutor;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * async-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/async")
public class AsyncTestController extends AsyncDemoController {
    public AsyncTestController(AsyncTaskExecutor executor, ThreadLocalContextAdapter contextAdapter) {
        super(executor, contextAdapter);
    }
}
