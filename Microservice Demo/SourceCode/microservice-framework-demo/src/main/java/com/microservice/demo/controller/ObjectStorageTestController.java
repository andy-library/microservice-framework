package com.microservice.demo.controller;

import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * object-storage-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/object-storage")
public class ObjectStorageTestController extends StorageDemoController {

    public ObjectStorageTestController(ObjectStorageOperations objectStorageOperations,
                                       PreSignedUrlGenerator preSignedUrlGenerator) {
        super(objectStorageOperations, preSignedUrlGenerator);
    }
}
