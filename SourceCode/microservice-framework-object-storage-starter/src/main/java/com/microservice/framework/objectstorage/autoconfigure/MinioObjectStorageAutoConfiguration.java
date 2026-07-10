package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * MinIO 对象存储自动配置
 * <p>
 * 当类路径中存在 {@code io.minio.MinioClient} 时激活，
 * 提供基于 MinIO SDK 的 {@link ObjectStorageOperations} 和 {@link PreSignedUrlGenerator} 实现。
 * <p>
 * 此配置使用 {@code @ConditionalOnClass(name=...)} 字符串形式，因为 MinIO SDK 为可选依赖，
 * 可能不在类路径中。此配置类不应注册在 {@code .imports} 文件中。
 * <p>
 * 当 AWS S3 SDK 和 MinIO SDK 同时存在时，AWS S3 优先（{@link S3ObjectStorageAutoConfiguration}
 * 的 Bean 先注册），MinIO 仅在用户未自定义且 S3 Bean 未创建时激活。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.minio.MinioClient")
@ConditionalOnProperty(prefix = "framework.object-storage", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class MinioObjectStorageAutoConfiguration {

    /**
     * 提供 MinioObjectStorageOperations Bean
     * <p>
     * 仅当容器中不存在其他 {@link ObjectStorageOperations} 实现时创建，
     * 允许用户自定义覆盖，也允许 S3 优先。
     *
     * @param properties 对象存储配置属性
     * @return MinioObjectStorageOperations 实例
     */
    @Bean("minioObjectStorageOperations")
    @ConditionalOnMissingBean(ObjectStorageOperations.class)
    public ObjectStorageOperations minioObjectStorageOperations(ObjectStorageProperties properties) {
        return new MinioObjectStorageOperations(properties);
    }

    /**
     * 提供 MinioPreSignedUrlGenerator Bean
     * <p>
     * 仅当容器中不存在其他 {@link PreSignedUrlGenerator} 实现时创建，
     * 仅在预签名功能启用时激活。
     *
     * @param properties 对象存储配置属性
     * @return MinioPreSignedUrlGenerator 实例
     */
    @Bean("minioPreSignedUrlGenerator")
    @ConditionalOnMissingBean(PreSignedUrlGenerator.class)
    @ConditionalOnProperty(prefix = "framework.object-storage.presign", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public PreSignedUrlGenerator minioPreSignedUrlGenerator(ObjectStorageProperties properties) {
        return new MinioPreSignedUrlGenerator(properties);
    }
}
