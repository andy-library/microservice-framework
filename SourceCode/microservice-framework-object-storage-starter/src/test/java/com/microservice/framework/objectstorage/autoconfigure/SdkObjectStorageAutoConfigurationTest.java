package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

class SdkObjectStorageAutoConfigurationTest {

    private final ApplicationContextRunner s3Runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObjectStorageAutoConfiguration.class,
                    S3ObjectStorageAutoConfiguration.class));

    private final ApplicationContextRunner minioRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader("software.amazon.awssdk.services.s3"))
            .withConfiguration(AutoConfigurations.of(
                    ObjectStorageAutoConfiguration.class,
                    MinioObjectStorageAutoConfiguration.class));

    @Test
    @DisplayName("S3 auto-configuration should fail fast when credentials are missing")
    void s3AutoConfigurationShouldFailFastWhenCredentialsAreMissing() {
        s3Runner.withPropertyValues("framework.object-storage.connection.bucket=prod-bucket")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("framework.object-storage.connection.access-key")
                            .hasMessageNotContaining("secret");
                });
    }

    @Test
    @DisplayName("S3 auto-configuration should create secure SDK-backed beans")
    void s3AutoConfigurationShouldCreateSecureSdkBackedBeans() {
        s3Runner.withPropertyValues(
                        "framework.object-storage.connection.bucket=prod-bucket",
                        "framework.object-storage.connection.access-key=access",
                        "framework.object-storage.connection.secret-key=super-secret",
                        "framework.object-storage.connection.region=us-east-1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(S3Client.class);
                    assertThat(context).hasSingleBean(S3Presigner.class);
                    assertThat(context).hasSingleBean(ObjectStorageOperations.class);
                    assertThat(context).hasSingleBean(PreSignedUrlGenerator.class);
                    assertThat(context.getBean(ObjectStorageOperations.class))
                            .isInstanceOf(S3ObjectStorageOperations.class);
                });
    }

    @Test
    @DisplayName("MinIO auto-configuration should fail fast when bucket is missing")
    void minioAutoConfigurationShouldFailFastWhenBucketIsMissing() {
        minioRunner.withPropertyValues(
                        "framework.object-storage.connection.endpoint=http://localhost:9000",
                        "framework.object-storage.connection.access-key=access",
                        "framework.object-storage.connection.secret-key=super-secret")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("framework.object-storage.connection.bucket")
                            .hasMessageNotContaining("super-secret");
                });
    }

    @Test
    @DisplayName("MinIO auto-configuration should create SDK-backed beans when S3 is absent")
    void minioAutoConfigurationShouldCreateSdkBackedBeansWhenS3IsAbsent() {
        minioRunner.withPropertyValues(
                        "framework.object-storage.connection.endpoint=http://localhost:9000",
                        "framework.object-storage.connection.bucket=prod-bucket",
                        "framework.object-storage.connection.access-key=access",
                        "framework.object-storage.connection.secret-key=super-secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MinioClient.class);
                    assertThat(context).hasSingleBean(ObjectStorageOperations.class);
                    assertThat(context).hasSingleBean(PreSignedUrlGenerator.class);
                    assertThat(context.getBean(ObjectStorageOperations.class))
                            .isInstanceOf(MinioObjectStorageOperations.class);
                });
    }
}
