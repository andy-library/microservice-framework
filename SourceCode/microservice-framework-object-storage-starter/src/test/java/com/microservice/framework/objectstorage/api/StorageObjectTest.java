package com.microservice.framework.objectstorage.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StorageObject 测试
 * <p>
 * 验证不可变对象的创建、相等性、元数据不可变性和空值处理。
 *
 * @author Andy Yang
 */
class StorageObjectTest {

    // ======================================================================
    // 创建测试
    // ======================================================================

    @Nested
    @DisplayName("创建 StorageObject")
    class Creation {

        @Test
        @DisplayName("使用完整参数创建 StorageObject")
        void shouldCreateWithFullParams() {
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("author", "framework");
            metadata.put("version", "1.0");

            Instant now = Instant.now();
            StorageObject obj = new StorageObject(
                    "docs/report.pdf", "my-bucket", 1024,
                    "application/pdf", now, metadata, "\"abc123\"");

            assertThat(obj.getKey()).isEqualTo("docs/report.pdf");
            assertThat(obj.getBucket()).isEqualTo("my-bucket");
            assertThat(obj.getSize()).isEqualTo(1024);
            assertThat(obj.getContentType()).isEqualTo("application/pdf");
            assertThat(obj.getLastModified()).isEqualTo(now);
            assertThat(obj.getETag()).isEqualTo("\"abc123\"");
        }

        @Test
        @DisplayName("key 为 null 时应抛出 NullPointerException")
        void shouldThrowWhenKeyIsNull() {
            assertThatThrownBy(() -> new StorageObject(
                    null, "bucket", 0, null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("key");
        }

        @Test
        @DisplayName("bucket 为 null 时应抛出 NullPointerException")
        void shouldThrowWhenBucketIsNull() {
            assertThatThrownBy(() -> new StorageObject(
                    "key", null, 0, null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("bucket");
        }

        @Test
        @DisplayName("可选参数为 null 时应使用默认值")
        void shouldUseDefaultsWhenOptionalParamsNull() {
            StorageObject obj = new StorageObject(
                    "key", "bucket", 0, null, null, null, null);

            assertThat(obj.getContentType()).isNull();
            assertThat(obj.getLastModified()).isNull();
            assertThat(obj.getMetadata()).isEmpty();
            assertThat(obj.getETag()).isNull();
        }
    }

    // ======================================================================
    // 相等性测试
    // ======================================================================

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同内容的两个对象应相等")
        void shouldBeEqualWhenSameContent() {
            Instant now = Instant.now();
            Map<String, String> meta = Map.of("k", "v");

            StorageObject obj1 = new StorageObject("key", "bucket", 100, "text/plain", now, meta, "etag");
            StorageObject obj2 = new StorageObject("key", "bucket", 100, "text/plain", now, meta, "etag");

            assertThat(obj1).isEqualTo(obj2);
            assertThat(obj1.hashCode()).isEqualTo(obj2.hashCode());
        }

        @Test
        @DisplayName("不同 key 的两个对象应不相等")
        void shouldNotBeEqualWhenDifferentKey() {
            StorageObject obj1 = new StorageObject("key1", "bucket", 0, null, null, null, null);
            StorageObject obj2 = new StorageObject("key2", "bucket", 0, null, null, null, null);

            assertThat(obj1).isNotEqualTo(obj2);
        }

        @Test
        @DisplayName("不同 bucket 的两个对象应不相等")
        void shouldNotBeEqualWhenDifferentBucket() {
            StorageObject obj1 = new StorageObject("key", "bucket1", 0, null, null, null, null);
            StorageObject obj2 = new StorageObject("key", "bucket2", 0, null, null, null, null);

            assertThat(obj1).isNotEqualTo(obj2);
        }

        @Test
        @DisplayName("与 null 比较应不相等")
        void shouldNotBeEqualToNull() {
            StorageObject obj = new StorageObject("key", "bucket", 0, null, null, null, null);
            assertThat(obj).isNotEqualTo(null);
        }

        @Test
        @DisplayName("与不同类型比较应不相等")
        void shouldNotBeEqualToDifferentType() {
            StorageObject obj = new StorageObject("key", "bucket", 0, null, null, null, null);
            assertThat(obj).isNotEqualTo("key");
        }

        @Test
        @DisplayName("同一对象应与自身相等")
        void shouldBeEqualToSelf() {
            StorageObject obj = new StorageObject("key", "bucket", 0, null, null, null, null);
            assertThat(obj).isEqualTo(obj);
        }
    }

    // ======================================================================
    // 元数据不可变性测试
    // ======================================================================

    @Nested
    @DisplayName("元数据不可变性")
    class MetadataImmutability {

        @Test
        @DisplayName("返回的元数据 Map 应为不可变")
        void shouldReturnImmutableMetadata() {
            Map<String, String> meta = new LinkedHashMap<>();
            meta.put("key1", "value1");

            StorageObject obj = new StorageObject("key", "bucket", 0, null, null, meta, null);
            Map<String, String> returnedMeta = obj.getMetadata();

            assertThatThrownBy(() -> returnedMeta.put("key2", "value2"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("原始元数据修改不应影响 StorageObject")
        void shouldNotBeAffectedByOriginalMetadataModification() {
            Map<String, String> meta = new LinkedHashMap<>();
            meta.put("key1", "value1");

            StorageObject obj = new StorageObject("key", "bucket", 0, null, null, meta, null);
            meta.put("key2", "value2");

            assertThat(obj.getMetadata()).hasSize(1);
            assertThat(obj.getMetadata()).containsEntry("key1", "value1");
            assertThat(obj.getMetadata()).doesNotContainKey("key2");
        }

        @Test
        @DisplayName("metadata 为 null 时返回空不可变 Map")
        void shouldReturnEmptyImmutableMapWhenMetadataNull() {
            StorageObject obj = new StorageObject("key", "bucket", 0, null, null, null, null);
            assertThat(obj.getMetadata()).isEmpty();

            assertThatThrownBy(() -> obj.getMetadata().put("k", "v"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ======================================================================
    // toString 测试
    // ======================================================================

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("toString 应包含所有字段信息")
        void shouldContainAllFields() {
            StorageObject obj = new StorageObject("report.pdf", "data", 2048,
                    "application/pdf", Instant.now(), null, "etag123");

            String str = obj.toString();
            assertThat(str).contains("report.pdf");
            assertThat(str).contains("data");
            assertThat(str).contains("2048");
            assertThat(str).contains("application/pdf");
            assertThat(str).contains("etag123");
        }
    }
}
