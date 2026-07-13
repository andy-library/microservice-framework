package com.microservice.demo.controller;

import com.microservice.framework.objectstorage.api.ObjectStorageException;
import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import com.microservice.framework.objectstorage.api.StorageObject;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * object-storage-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/storage")
public class StorageDemoController {

    private final ObjectStorageOperations objectStorageOperations;
    private final PreSignedUrlGenerator preSignedUrlGenerator;

    public StorageDemoController(ObjectStorageOperations objectStorageOperations,
                                 PreSignedUrlGenerator preSignedUrlGenerator) {
        this.objectStorageOperations = objectStorageOperations;
        this.preSignedUrlGenerator = preSignedUrlGenerator;
    }

    @PostMapping("/object")
    public ApiResponse<Map<String, Object>> put(@RequestBody(required = false) Map<String, Object> body) {
        String key = body == null ? UUID.randomUUID().toString() : String.valueOf(body.getOrDefault("key", UUID.randomUUID().toString()));
        String content = body == null ? "" : String.valueOf(body.getOrDefault("content", ""));
        StorageObject stored = objectStorageOperations.upload(key,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), "text/plain");
        return ApiResponse.success(Map.of("key", stored.getKey(), "objectKey", stored.getKey(),
                "stored", true, "operation", "upload", "size", stored.getSize(),
                "implementation", objectStorageOperations.getImplementationName()));
    }

    @GetMapping("/object/{key}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String key) {
        if (!objectStorageOperations.exists(key)) {
            return ApiResponse.success(Map.of("objectKey", key, "found", false, "operation", "download",
                    "implementation", objectStorageOperations.getImplementationName()));
        }
        try {
            String content = new String(objectStorageOperations.download(key).readAllBytes(), StandardCharsets.UTF_8);
            return ApiResponse.success(Map.of("objectKey", key, "found", true, "operation", "download",
                    "content", content, "size", objectStorageOperations.getSize(key),
                    "implementation", objectStorageOperations.getImplementationName()));
        } catch (java.io.IOException e) {
            throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                    "Failed to read object content: " + key, e);
        }
    }

    @GetMapping("/object/{key}/presigned-url")
    public ApiResponse<Map<String, Object>> presigned(@PathVariable String key) {
        String downloadUrl = preSignedUrlGenerator.generateDownloadUrl(key);
        String uploadUrl = preSignedUrlGenerator.generateUploadUrl(key);
        return ApiResponse.success(Map.of("objectKey", key,
                "url", downloadUrl, "downloadUrl", downloadUrl, "uploadUrl", uploadUrl,
                "implementation", preSignedUrlGenerator.getImplementationName()));
    }

    @DeleteMapping("/object/{key}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable String key) {
        boolean existed = objectStorageOperations.exists(key);
        if (existed) {
            objectStorageOperations.delete(key);
        }
        return ApiResponse.success(Map.of("objectKey", key, "operation", "delete",
                "deleted", existed, "existsBeforeDelete", existed,
                "existsAfterDelete", objectStorageOperations.exists(key),
                "implementation", objectStorageOperations.getImplementationName()));
    }
}
