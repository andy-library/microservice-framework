package com.microservice.framework.objectstorage.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PreSignedUrlGenerator 接口契约测试
 * <p>
 * 验证接口的方法签名和文档约定，确保所有方法声明完整且正确。
 *
 * @author Andy Yang
 */
class PreSignedUrlGeneratorTest {

    // ======================================================================
    // 接口方法签名验证
    // ======================================================================

    @Nested
    @DisplayName("接口方法签名验证")
    class MethodSignatureVerification {

        @Test
        @DisplayName("PreSignedUrlGenerator 应声明 generateUploadUrl 方法（含 bucket 参数）")
        void shouldDeclareGenerateUploadUrlWithBucket() {
            assertThat(PreSignedUrlGenerator.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("generateUploadUrl")
                            && m.getParameterCount() == 2);
        }

        @Test
        @DisplayName("PreSignedUrlGenerator 应声明 generateUploadUrl 方法（使用默认 bucket）")
        void shouldDeclareGenerateUploadUrlWithoutBucket() {
            assertThat(PreSignedUrlGenerator.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("generateUploadUrl")
                            && m.getParameterCount() == 1);
        }

        @Test
        @DisplayName("PreSignedUrlGenerator 应声明 generateDownloadUrl 方法（含 bucket 参数）")
        void shouldDeclareGenerateDownloadUrlWithBucket() {
            assertThat(PreSignedUrlGenerator.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("generateDownloadUrl")
                            && m.getParameterCount() == 2);
        }

        @Test
        @DisplayName("PreSignedUrlGenerator 应声明 generateDownloadUrl 方法（使用默认 bucket）")
        void shouldDeclareGenerateDownloadUrlWithoutBucket() {
            assertThat(PreSignedUrlGenerator.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("generateDownloadUrl")
                            && m.getParameterCount() == 1);
        }

        @Test
        @DisplayName("PreSignedUrlGenerator 应声明 generateUrl 方法（Duration 参数）")
        void shouldDeclareGenerateUrlWithDuration() throws NoSuchMethodException {
            var method = PreSignedUrlGenerator.class.getDeclaredMethod(
                    "generateUrl", String.class, String.class, Duration.class);
            assertThat(method).isNotNull();
            assertThat(method.getParameterTypes()[2]).isEqualTo(Duration.class);
        }

        @Test
        @DisplayName("PreSignedUrlGenerator 应声明 generateUrl 方法（Instant 参数）")
        void shouldDeclareGenerateUrlWithInstant() throws NoSuchMethodException {
            var method = PreSignedUrlGenerator.class.getDeclaredMethod(
                    "generateUrl", String.class, String.class, Instant.class);
            assertThat(method).isNotNull();
            assertThat(method.getParameterTypes()[2]).isEqualTo(Instant.class);
        }

        @Test
        @DisplayName("PreSignedUrlGenerator 应声明 getImplementationName 方法")
        void shouldDeclareGetImplementationName() {
            assertThat(PreSignedUrlGenerator.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("getImplementationName"));
        }
    }

    // ======================================================================
    // 接口约定验证
    // ======================================================================

    @Nested
    @DisplayName("接口约定验证")
    class ContractVerification {

        @Test
        @DisplayName("PreSignedUrlGenerator 应为接口")
        void shouldBeInterface() {
            assertThat(PreSignedUrlGenerator.class.isInterface()).isTrue();
        }

        @Test
        @DisplayName("所有 URL 生成方法返回类型应为 String")
        void urlMethodsShouldReturnString() throws NoSuchMethodException {
            assertThat(PreSignedUrlGenerator.class.getDeclaredMethod(
                    "generateUploadUrl", String.class, String.class).getReturnType())
                    .isEqualTo(String.class);

            assertThat(PreSignedUrlGenerator.class.getDeclaredMethod(
                    "generateDownloadUrl", String.class, String.class).getReturnType())
                    .isEqualTo(String.class);

            assertThat(PreSignedUrlGenerator.class.getDeclaredMethod(
                    "generateUrl", String.class, String.class, Duration.class).getReturnType())
                    .isEqualTo(String.class);

            assertThat(PreSignedUrlGenerator.class.getDeclaredMethod(
                    "generateUrl", String.class, String.class, Instant.class).getReturnType())
                    .isEqualTo(String.class);
        }

        @Test
        @DisplayName("getImplementationName 返回类型应为 String")
        void getImplementationNameShouldReturnString() throws NoSuchMethodException {
            assertThat(PreSignedUrlGenerator.class.getDeclaredMethod(
                    "getImplementationName").getReturnType())
                    .isEqualTo(String.class);
        }
    }
}
