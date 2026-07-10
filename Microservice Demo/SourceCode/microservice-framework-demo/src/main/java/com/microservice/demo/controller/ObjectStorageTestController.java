package com.microservice.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * object-storage-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/object-storage")
public class ObjectStorageTestController extends StorageDemoController {
}
