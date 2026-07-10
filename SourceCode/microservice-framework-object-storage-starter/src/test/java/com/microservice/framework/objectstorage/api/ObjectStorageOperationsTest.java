package com.microservice.framework.objectstorage.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ObjectStorageOperations 接口契约测试
 * <p>
 * 验证接口的方法签名和文档约定，确保所有方法声明完整且正确。
 *
 * @author Andy Yang
 */
class ObjectStorageOperationsTest {

    // ======================================================================
    // 接口方法签名验证
    // ======================================================================

    @Nested
    @DisplayName("接口方法签名验证")
    class MethodSignatureVerification {

        @Test
        @DisplayName("ObjectStorageOperations 应声明 upload 方法（含 bucket 参数）")
        void shouldDeclareUploadWithBucket() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("upload")
                            && m.getParameterCount() == 4);
        }

        @Test
        @DisplayName("ObjectStorageOperations 应声明 upload 方法（使用默认 bucket）")
        void shouldDeclareUploadWithoutBucket() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("upload")
                            && m.getParameterCount() == 3);
        }

        @Test
        @DisplayName("ObjectStorageOperations 应声明 download 方法")
        void shouldDeclareDownload() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("download"));
        }

        @Test
        @DisplayName("ObjectStorageOperations 应声明 delete 方法")
        void shouldDeclareDelete() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("delete"));
        }

        @Test
        @DisplayName("ObjectStorageOperations 应声明 exists 方法")
        void shouldDeclareExists() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("exists"));
        }

        @Test
        @DisplayName("ObjectStorageOperations 应声明 list 方法")
        void shouldDeclareList() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("list"));
        }

        @Test
        @DisplayName("ObjectStorageOperations 应声明 copy 方法")
        void shouldDeclareCopy() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("copy"));
        }

        @Test
        @DisplayName("ObjectStorageOperations 应声明 move 方法")
        void shouldDeclareMove() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("move"));
        }

        @Test
        @DisplayName("ObjectStorageOperations 应声明 getMetadata 方法")
        void shouldDeclareGetMetadata() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("getMetadata"));
        }

        @Test
        @DisplayName("ObjectStorageOperations 应声明 getSize 方法")
        void shouldDeclareGetSize() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("getSize"));
        }

        @Test
        @DisplayName("ObjectStorageOperations 应声明 getImplementationName 方法")
        void shouldDeclareGetImplementationName() {
            assertThat(ObjectStorageOperations.class.getDeclaredMethods())
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
        @DisplayName("ObjectStorageOperations 应为接口")
        void shouldBeInterface() {
            assertThat(ObjectStorageOperations.class.isInterface()).isTrue();
        }

        @Test
        @DisplayName("upload 返回类型应为 StorageObject")
        void uploadShouldReturnStorageObject() throws NoSuchMethodException {
            var method = ObjectStorageOperations.class.getDeclaredMethod(
                    "upload", String.class, String.class, java.io.InputStream.class, String.class);
            assertThat(method.getReturnType()).isEqualTo(StorageObject.class);
        }

        @Test
        @DisplayName("download 返回类型应为 InputStream")
        void downloadShouldReturnInputStream() throws NoSuchMethodException {
            var method = ObjectStorageOperations.class.getDeclaredMethod(
                    "download", String.class, String.class);
            assertThat(method.getReturnType()).isEqualTo(java.io.InputStream.class);
        }

        @Test
        @DisplayName("exists 返回类型应为 boolean")
        void existsShouldReturnBoolean() throws NoSuchMethodException {
            var method = ObjectStorageOperations.class.getDeclaredMethod(
                    "exists", String.class, String.class);
            assertThat(method.getReturnType()).isEqualTo(boolean.class);
        }

        @Test
        @DisplayName("list 返回类型应为 List")
        void listShouldReturnList() throws NoSuchMethodException {
            var method = ObjectStorageOperations.class.getDeclaredMethod(
                    "list", String.class, String.class);
            assertThat(method.getReturnType()).isEqualTo(java.util.List.class);
        }

        @Test
        @DisplayName("getSize 返回类型应为 long")
        void getSizeShouldReturnLong() throws NoSuchMethodException {
            var method = ObjectStorageOperations.class.getDeclaredMethod(
                    "getSize", String.class, String.class);
            assertThat(method.getReturnType()).isEqualTo(long.class);
        }
    }
}
