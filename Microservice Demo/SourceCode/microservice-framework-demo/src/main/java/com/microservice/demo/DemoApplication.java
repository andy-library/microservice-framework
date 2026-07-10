package com.microservice.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Microservice Framework Demo Application
 * 
 * 演示项目主启动类，用于验证 Microservice Framework 的所有能力：
 * 1. 分布式追踪（Observability Starter）
 * 2. 结构化日志（Logging Starter）
 * 3. 数据脱敏
 * 4. 日志限流
 * 5. Trace 关联采样
 * 
 * @author Andy Yang
 */
@SpringBootApplication
@EnableScheduling
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
