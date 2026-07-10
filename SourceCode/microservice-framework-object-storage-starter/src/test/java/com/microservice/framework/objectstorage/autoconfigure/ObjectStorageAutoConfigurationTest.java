package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import com.microservice.framework.objectstorage.api.StorageObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Object Storage 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证自动配置的激活条件、
 * 属性绑定和用户自定义 Bean 覆盖默认 Bean 的行为。
 * <p>
 * 注意：由于 AWS S3 SDK 和 MinIO SDK 为可选依赖且不在测试类路径中，
 * S3ObjectStorageAutoConfiguration 和 MinioObjectStorageAutoConfiguration
 * 的 @ConditionalOnClass 条件不会被满足，因此相关 Bean 不会创建。
 * 测试重点验证：
 * <ul>
 *   <li>默认配置下 ObjectStorageAutoConfiguration 激活</li>
 *   <li>配置 enabled=false 时自动配置不激活</li>
 *   <li>缺少 SDK 依赖时不创建 ObjectStorageOperations/PreSignedUrlGenerator Bean</li>
 *   <li>用户自定义 Bean 覆盖行为</li>
 * </ul>
 *
 * @author Andy Yang
 */
class ObjectStorageAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObjectStorageAutoConfiguration.class));

    // ======================================================================
    // 默认激活
    // ======================================================================

    @Nested
    @DisplayName("默认配置激活")
    class DefaultActivation {

        @Test
        @DisplayName("默认配置应激活 ObjectStorageAutoConfiguration")
        void defaultShouldActivateAutoConfiguration() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(ObjectStorageProperties.class);
            });
        }

        @Test
        @DisplayName("默认配置应创建 ObjectStorageProperties Bean")
        void defaultShouldCreatePropertiesBean() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                ObjectStorageProperties props = context.getBean(ObjectStorageProperties.class);
                assertThat(props.getEnabled()).isTrue();
                assertThat(props.getConnection().getEndpoint())
                        .isEqualTo("https://s3.amazonaws.com");
            });
        }
    }

    // ======================================================================
    // enabled=false 禁用
    // ======================================================================

    @Nested
    @DisplayName("enabled=false 禁用")
    class DisabledConfiguration {

        @Test
        @DisplayName("配置 enabled=false 时不应激活自动配置")
        void shouldNotActivateWhenDisabled() {
            contextRunner.withPropertyValues("framework.object-storage.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(ObjectStorageProperties.class);
                    });
        }
    }

    // ======================================================================
    // 缺少 SDK 依赖
    // ======================================================================

    @Nested
    @DisplayName("缺少 SDK 依赖时不创建存储操作 Bean")
    class MissingSdkDependency {

        @Test
        @DisplayName("缺少 S3/MinIO SDK 时不应创建 ObjectStorageOperations Bean")
        void shouldNotCreateOperationsWithoutSdk() {
            contextRunner.run(context -> {
                assertThat(context).doesNotHaveBean(ObjectStorageOperations.class);
            });
        }

        @Test
        @DisplayName("缺少 S3/MinIO SDK 时不应创建 PreSignedUrlGenerator Bean")
        void shouldNotCreatePreSignedUrlGeneratorWithoutSdk() {
            contextRunner.run(context -> {
                assertThat(context).doesNotHaveBean(PreSignedUrlGenerator.class);
            });
        }
    }

    // ======================================================================
    // 属性绑定
    // ======================================================================

    @Nested
    @DisplayName("属性绑定")
    class PropertyBinding {

        @Test
        @DisplayName("应正确绑定 connection.endpoint 属性")
        void shouldBindConnectionEndpoint() {
            contextRunner.withPropertyValues(
                    "framework.object-storage.connection.endpoint=http://localhost:9000")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ObjectStorageProperties props = context.getBean(ObjectStorageProperties.class);
                        assertThat(props.getConnection().getEndpoint())
                                .isEqualTo("http://localhost:9000");
                    });
        }

        @Test
        @DisplayName("应正确绑定 connection.bucket 属性")
        void shouldBindConnectionBucket() {
            contextRunner.withPropertyValues(
                    "framework.object-storage.connection.bucket=my-bucket")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ObjectStorageProperties props = context.getBean(ObjectStorageProperties.class);
                        assertThat(props.getConnection().getBucket()).isEqualTo("my-bucket");
                    });
        }

        @Test
        @DisplayName("应正确绑定 upload.max-file-size 属性")
        void shouldBindUploadMaxFileSize() {
            contextRunner.withPropertyValues(
                    "framework.object-storage.upload.max-file-size=10485760")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ObjectStorageProperties props = context.getBean(ObjectStorageProperties.class);
                        assertThat(props.getUpload().getMaxFileSize()).isEqualTo(10_485_760);
                    });
        }

        @Test
        @DisplayName("应正确绑定 presign.default-expiry 属性")
        void shouldBindPresignDefaultExpiry() {
            contextRunner.withPropertyValues(
                    "framework.object-storage.presign.default-expiry=7200")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ObjectStorageProperties props = context.getBean(ObjectStorageProperties.class);
                        assertThat(props.getPresign().getDefaultExpiry()).isEqualTo(7200);
                    });
        }

        @Test
        @DisplayName("应正确绑定 governance.quarantine-enabled 属性")
        void shouldBindGovernanceQuarantineEnabled() {
            contextRunner.withPropertyValues(
                    "framework.object-storage.governance.quarantine-enabled=true")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ObjectStorageProperties props = context.getBean(ObjectStorageProperties.class);
                        assertThat(props.getGovernance().getQuarantineEnabled()).isTrue();
                    });
        }
    }

    // ======================================================================
    // 用户自定义 Bean 覆盖
    // ======================================================================

    @Nested
    @DisplayName("用户自定义 Bean 覆盖")
    class UserProvidedBeanOverride {

        @Test
        @DisplayName("用户提供的 ObjectStorageOperations 应被识别")
        void userProvidedOperationsShouldBeRecognized() {
            ObjectStorageOperations customOps = new ObjectStorageOperations() {
                @Override public StorageObject upload(String bucket, String key, java.io.InputStream is, String ct) { return null; }
                @Override public StorageObject upload(String key, java.io.InputStream is, String ct) { return null; }
                @Override public java.io.InputStream download(String bucket, String key) { return null; }
                @Override public java.io.InputStream download(String key) { return null; }
                @Override public void delete(String bucket, String key) {}
                @Override public void delete(String key) {}
                @Override public boolean exists(String bucket, String key) { return false; }
                @Override public boolean exists(String key) { return false; }
                @Override public java.util.List<StorageObject> list(String bucket, String prefix) { return java.util.Collections.emptyList(); }
                @Override public java.util.List<StorageObject> list(String prefix) { return java.util.Collections.emptyList(); }
                @Override public StorageObject copy(String sb, String sk, String db, String dk) { return null; }
                @Override public StorageObject copy(String sk, String dk) { return null; }
                @Override public StorageObject move(String sb, String sk, String db, String dk) { return null; }
                @Override public StorageObject move(String sk, String dk) { return null; }
                @Override public java.util.Map<String, String> getMetadata(String bucket, String key) { return java.util.Collections.emptyMap(); }
                @Override public java.util.Map<String, String> getMetadata(String key) { return java.util.Collections.emptyMap(); }
                @Override public long getSize(String bucket, String key) { return 0; }
                @Override public long getSize(String key) { return 0; }
                @Override public String getImplementationName() { return "custom"; }
            };

            contextRunner.withBean("customObjectStorageOperations",
                    ObjectStorageOperations.class, () -> customOps)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customObjectStorageOperations");
                        assertThat(context.getBean(ObjectStorageOperations.class)
                                .getImplementationName()).isEqualTo("custom");
                    });
        }

        @Test
        @DisplayName("用户提供的 PreSignedUrlGenerator 应被识别")
        void userProvidedPreSignedUrlGeneratorShouldBeRecognized() {
            PreSignedUrlGenerator customGen = new PreSignedUrlGenerator() {
                @Override public String generateUploadUrl(String bucket, String key) { return "http://custom-upload"; }
                @Override public String generateUploadUrl(String key) { return "http://custom-upload"; }
                @Override public String generateDownloadUrl(String bucket, String key) { return "http://custom-download"; }
                @Override public String generateDownloadUrl(String key) { return "http://custom-download"; }
                @Override public String generateUrl(String bucket, String key, java.time.Duration expiry) { return "http://custom-url"; }
                @Override public String generateUrl(String key, java.time.Duration expiry) { return "http://custom-url"; }
                @Override public String generateUrl(String bucket, String key, java.time.Instant expiration) { return "http://custom-url"; }
                @Override public String getImplementationName() { return "custom"; }
            };

            contextRunner.withBean("customPreSignedUrlGenerator",
                    PreSignedUrlGenerator.class, () -> customGen)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customPreSignedUrlGenerator");
                        assertThat(context.getBean(PreSignedUrlGenerator.class)
                                .getImplementationName()).isEqualTo("custom");
                    });
        }
    }
}
