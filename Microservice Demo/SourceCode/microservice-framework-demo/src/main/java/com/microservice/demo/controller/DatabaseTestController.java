package com.microservice.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * database-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/database")
public class DatabaseTestController extends DatabaseDemoController {
}
