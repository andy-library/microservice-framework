package com.microservice.demo.controller;

import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import com.microservice.framework.common.id.IdGenerator;
import com.microservice.framework.common.time.FrameworkClock;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * common-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/common")
public class CommonTestController extends CommonDemoController {

    public CommonTestController(IdGenerator idGenerator, FrameworkClock frameworkClock,
                                ThreadLocalContextAdapter contextAdapter) {
        super(idGenerator, frameworkClock, contextAdapter);
    }
}
