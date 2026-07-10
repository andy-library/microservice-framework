package com.microservice.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * logging-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/logging")
public class LoggingTestController extends LogDemoController {
}
