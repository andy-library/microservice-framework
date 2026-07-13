package com.microservice.demo.controller;

import com.microservice.framework.xxljob.api.IdempotentJobHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * xxl-job-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/xxl-job")
public class XxlJobTestController extends JobDemoController {
    public XxlJobTestController(IdempotentJobHandler handler) { super(handler); }
}
