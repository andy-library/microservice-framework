package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Object Storage 自动配置入口
 * <p>
 * 当类路径中存在 S3 SDK（{@code software.amazon.awssdk.services.s3.S3Client})
 * 或 MinIO SDK（{@code io.minio.MinioClient}) 时激活，
 * 提供 {@link ObjectStorageOperations} 和 {@link PreSignedUrlGenerator} Bean。
 * <p>
 * 两个 SDK 为可选依赖，通过 {@code @ConditionalOnClass} 进行运行时检测：
 * <ul>
 *   <li>AWS S3 SDK 在类路径时，由 {@link S3ObjectStorageAutoConfiguration} 提供实现</li>
 *   <li>MinIO SDK 在类路径时，由 {@link MinioObjectStorageAutoConfiguration} 提供实现</li>
 * </ul>
 * 如果两个 SDK 同时存在，AWS S3 优先。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(ObjectStorageProperties.class)
@ConditionalOnProperty(prefix = "framework.object-storage", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ObjectStorageAutoConfiguration {
}
