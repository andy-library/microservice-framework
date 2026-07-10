package com.microservice.framework.objectstorage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ObjectStorageProperties 测试
 * <p>
 * 验证配置属性的默认值、验证约束和嵌套配置结构。
 *
 * @author Andy Yang
 */
class ObjectStoragePropertiesTest {

    // ======================================================================
    // 默认值测试
    // ======================================================================

    @Nested
    @DisplayName("默认值验证")
    class Defaults {

        @Test
        @DisplayName("enabled 默认应为 true")
        void enabledDefaultShouldBeTrue() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("connection.endpoint 默认应为 https://s3.amazonaws.com")
        void connectionEndpointDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getConnection().getEndpoint())
                    .isEqualTo("https://s3.amazonaws.com");
        }

        @Test
        @DisplayName("connection.region 默认应为 us-east-1")
        void connectionRegionDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getConnection().getRegion()).isEqualTo("us-east-1");
        }

        @Test
        @DisplayName("connection.accessKey 默认应为 null")
        void connectionAccessKeyDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getConnection().getAccessKey()).isNull();
        }

        @Test
        @DisplayName("connection.secretKey 默认应为 null")
        void connectionSecretKeyDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getConnection().getSecretKey()).isNull();
        }

        @Test
        @DisplayName("connection.bucket 默认应为 null")
        void connectionBucketDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getConnection().getBucket()).isNull();
        }

        @Test
        @DisplayName("upload.maxFileSize 默认应为 50MB (52,428,800 字节)")
        void uploadMaxFileSizeDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getUpload().getMaxFileSize()).isEqualTo(52_428_800);
        }

        @Test
        @DisplayName("upload.allowedTypes 默认应为空列表")
        void uploadAllowedTypesDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getUpload().getAllowedTypes()).isEmpty();
        }

        @Test
        @DisplayName("upload.multipartThreshold 默认应为 5MB (5,242,880 字节)")
        void uploadMultipartThresholdDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getUpload().getMultipartThreshold()).isEqualTo(5_242_880);
        }

        @Test
        @DisplayName("presign.enabled 默认应为 true")
        void presignEnabledDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getPresign().getEnabled()).isTrue();
        }

        @Test
        @DisplayName("presign.defaultExpiry 默认应为 3600")
        void presignDefaultExpiryDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getPresign().getDefaultExpiry()).isEqualTo(3600);
        }

        @Test
        @DisplayName("governance.enabled 默认应为 true")
        void governanceEnabledDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getGovernance().getEnabled()).isTrue();
        }

        @Test
        @DisplayName("governance.quarantineEnabled 默认应为 false")
        void governanceQuarantineEnabledDefault() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getGovernance().getQuarantineEnabled()).isFalse();
        }
    }

    // ======================================================================
    // 属性绑定测试
    // ======================================================================

    @Nested
    @DisplayName("属性绑定")
    class PropertyBinding {

        @Test
        @DisplayName("修改 connection.endpoint 应生效")
        void shouldBindConnectionEndpoint() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            props.getConnection().setEndpoint("http://localhost:9000");
            assertThat(props.getConnection().getEndpoint()).isEqualTo("http://localhost:9000");
        }

        @Test
        @DisplayName("修改 connection.bucket 应生效")
        void shouldBindConnectionBucket() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            props.getConnection().setBucket("my-bucket");
            assertThat(props.getConnection().getBucket()).isEqualTo("my-bucket");
        }

        @Test
        @DisplayName("修改 upload.maxFileSize 应生效")
        void shouldBindUploadMaxFileSize() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            props.getUpload().setMaxFileSize(10_485_760);
            assertThat(props.getUpload().getMaxFileSize()).isEqualTo(10_485_760);
        }

        @Test
        @DisplayName("修改 upload.allowedTypes 应生效")
        void shouldBindUploadAllowedTypes() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            props.getUpload().setAllowedTypes(java.util.List.of("application/pdf", "image/png"));
            assertThat(props.getUpload().getAllowedTypes())
                    .containsExactly("application/pdf", "image/png");
        }

        @Test
        @DisplayName("修改 presign.defaultExpiry 应生效")
        void shouldBindPresignDefaultExpiry() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            props.getPresign().setDefaultExpiry(7200);
            assertThat(props.getPresign().getDefaultExpiry()).isEqualTo(7200);
        }

        @Test
        @DisplayName("修改 governance.quarantineEnabled 应生效")
        void shouldBindGovernanceQuarantineEnabled() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            props.getGovernance().setQuarantineEnabled(true);
            assertThat(props.getGovernance().getQuarantineEnabled()).isTrue();
        }

        @Test
        @DisplayName("修改 enabled 应生效")
        void shouldBindEnabled() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            props.setEnabled(false);
            assertThat(props.getEnabled()).isFalse();
        }
    }

    // ======================================================================
    // 嵌套配置结构测试
    // ======================================================================

    @Nested
    @DisplayName("嵌套配置结构")
    class NestedStructure {

        @Test
        @DisplayName("ObjectStorageProperties 应包含 ConnectionProperties")
        void shouldContainConnectionProperties() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getConnection()).isNotNull();
            assertThat(props.getConnection())
                    .isInstanceOf(ObjectStorageProperties.ConnectionProperties.class);
        }

        @Test
        @DisplayName("ObjectStorageProperties 应包含 UploadProperties")
        void shouldContainUploadProperties() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getUpload()).isNotNull();
            assertThat(props.getUpload())
                    .isInstanceOf(ObjectStorageProperties.UploadProperties.class);
        }

        @Test
        @DisplayName("ObjectStorageProperties 应包含 PresignProperties")
        void shouldContainPresignProperties() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getPresign()).isNotNull();
            assertThat(props.getPresign())
                    .isInstanceOf(ObjectStorageProperties.PresignProperties.class);
        }

        @Test
        @DisplayName("ObjectStorageProperties 应包含 GovernanceProperties")
        void shouldContainGovernanceProperties() {
            ObjectStorageProperties props = new ObjectStorageProperties();
            assertThat(props.getGovernance()).isNotNull();
            assertThat(props.getGovernance())
                    .isInstanceOf(ObjectStorageProperties.GovernanceProperties.class);
        }
    }

    // ======================================================================
    // 配置前缀测试
    // ======================================================================

    @Nested
    @DisplayName("配置前缀")
    class ConfigurationPrefix {

        @Test
        @DisplayName("ObjectStorageProperties 配置前缀应为 framework.object-storage")
        void shouldHaveCorrectPrefix() {
            var annotation = ObjectStorageProperties.class
                    .getAnnotation(org.springframework.boot.context.properties.ConfigurationProperties.class);
            assertThat(annotation).isNotNull();
            assertThat(annotation.prefix()).isEqualTo("framework.object-storage");
        }
    }
}
