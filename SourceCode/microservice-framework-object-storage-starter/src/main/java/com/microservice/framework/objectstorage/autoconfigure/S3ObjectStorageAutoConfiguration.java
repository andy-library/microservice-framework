package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * AWS S3 对象存储自动配置
 * <p>
 * 当类路径中存在 {@code software.amazon.awssdk.services.s3.S3Client} 时激活，
 * 提供基于 AWS S3 SDK 的 {@link ObjectStorageOperations} 和 {@link PreSignedUrlGenerator} 实现。
 * <p>
 * 此配置使用 {@code @ConditionalOnClass(name=...)} 字符串形式，因为 AWS S3 SDK 为可选依赖，
 * 可能不在类路径中。此配置类不应注册在 {@code .imports} 文件中。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnClass(name = "software.amazon.awssdk.services.s3.S3Client")
@ConditionalOnProperty(prefix = "framework.object-storage", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class S3ObjectStorageAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(S3Client.class)
    public S3Client s3Client(ObjectStorageProperties properties) {
        var connection = properties.getConnection();
        String accessKey = ObjectStorageSupport.requireProperty(connection.getAccessKey(),
                "framework.object-storage.connection.access-key");
        String secretKey = ObjectStorageSupport.requireProperty(connection.getSecretKey(),
                "framework.object-storage.connection.secret-key");
        String region = ObjectStorageSupport.requireProperty(connection.getRegion(),
                "framework.object-storage.connection.region");
        String endpoint = ObjectStorageSupport.requireProperty(connection.getEndpoint(),
                "framework.object-storage.connection.endpoint");
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(isNonAwsEndpoint(endpoint))
                        .build())
                .build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(S3Presigner.class)
    public S3Presigner s3Presigner(ObjectStorageProperties properties) {
        var connection = properties.getConnection();
        String accessKey = ObjectStorageSupport.requireProperty(connection.getAccessKey(),
                "framework.object-storage.connection.access-key");
        String secretKey = ObjectStorageSupport.requireProperty(connection.getSecretKey(),
                "framework.object-storage.connection.secret-key");
        String region = ObjectStorageSupport.requireProperty(connection.getRegion(),
                "framework.object-storage.connection.region");
        String endpoint = ObjectStorageSupport.requireProperty(connection.getEndpoint(),
                "framework.object-storage.connection.endpoint");
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(isNonAwsEndpoint(endpoint))
                        .build())
                .build();
    }

    /**
     * 提供 S3ObjectStorageOperations Bean
     * <p>
     * 仅当容器中不存在其他 {@link ObjectStorageOperations} 实现时创建，
     * 允许用户自定义覆盖。
     *
     * @param properties 对象存储配置属性
     * @return S3ObjectStorageOperations 实例
     */
    @Bean("s3ObjectStorageOperations")
    @ConditionalOnMissingBean(ObjectStorageOperations.class)
    public ObjectStorageOperations s3ObjectStorageOperations(S3Client s3Client,
                                                             ObjectStorageProperties properties) {
        ObjectStorageSupport.requireProperty(properties.getConnection().getBucket(),
                "framework.object-storage.connection.bucket");
        return new S3ObjectStorageOperations(s3Client, properties);
    }

    /**
     * 提供 S3PreSignedUrlGenerator Bean
     * <p>
     * 仅当容器中不存在其他 {@link PreSignedUrlGenerator} 实现时创建，
     * 仅在预签名功能启用时激活。
     *
     * @param properties 对象存储配置属性
     * @return S3PreSignedUrlGenerator 实例
     */
    @Bean("s3PreSignedUrlGenerator")
    @ConditionalOnMissingBean(PreSignedUrlGenerator.class)
    @ConditionalOnProperty(prefix = "framework.object-storage.presign", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public PreSignedUrlGenerator s3PreSignedUrlGenerator(S3Presigner s3Presigner,
                                                         ObjectStorageProperties properties) {
        ObjectStorageSupport.requireProperty(properties.getConnection().getBucket(),
                "framework.object-storage.connection.bucket");
        return new S3PreSignedUrlGenerator(s3Presigner, properties);
    }

    private static boolean isNonAwsEndpoint(String endpoint) {
        String host = URI.create(endpoint).getHost();
        return host == null || !host.endsWith("amazonaws.com");
    }
}
