package com.microservice.framework.apollo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SensitiveConfigMasker 测试
 * <p>
 * 验证敏感配置键判断、值脱敏处理和批量脱敏。
 * 关键安全要求：脱敏后的输出不应包含原始敏感值的明文。
 *
 * @author Andy Yang
 */
class SensitiveConfigMaskerTest {

    @Nested
    @DisplayName("敏感键判断 isSensitive()")
    class IsSensitiveTests {

        private final SensitiveConfigMasker masker = SensitiveConfigMasker.defaultMasker();

        @Test
        @DisplayName("包含 password 的键应为敏感键")
        void keyContainingPasswordShouldBeSensitive() {
            assertThat(masker.isSensitive("database.password")).isTrue();
            assertThat(masker.isSensitive("redis.password")).isTrue();
            assertThat(masker.isSensitive("app.password-reset")).isTrue();
        }

        @Test
        @DisplayName("包含 secret 的键应为敏感键")
        void keyContainingSecretShouldBeSensitive() {
            assertThat(masker.isSensitive("app.client-secret")).isTrue();
            assertThat(masker.isSensitive("jwt.secret")).isTrue();
        }

        @Test
        @DisplayName("包含 key 的键应为敏感键")
        void keyContainingKeyShouldBeSensitive() {
            assertThat(masker.isSensitive("api.key")).isTrue();
            assertThat(masker.isSensitive("encryption.key")).isTrue();
        }

        @Test
        @DisplayName("包含 token 的键应为敏感键")
        void keyContainingTokenShouldBeSensitive() {
            assertThat(masker.isSensitive("auth.token")).isTrue();
            assertThat(masker.isSensitive("access.token")).isTrue();
        }

        @Test
        @DisplayName("包含 credential 的键应为敏感键")
        void keyContainingCredentialShouldBeSensitive() {
            assertThat(masker.isSensitive("aws.credential")).isTrue();
        }

        @Test
        @DisplayName("包含 pwd 的键应为敏感键")
        void keyContainingPwdShouldBeSensitive() {
            assertThat(masker.isSensitive("login.pwd")).isTrue();
        }

        @Test
        @DisplayName("不包含敏感模式的键不应为敏感键")
        void keyNotContainingSensitivePatternShouldNotBeSensitive() {
            assertThat(masker.isSensitive("server.port")).isFalse();
            assertThat(masker.isSensitive("app.name")).isFalse();
            assertThat(masker.isSensitive("logging.level")).isFalse();
            assertThat(masker.isSensitive("database.url")).isFalse();
        }

        @Test
        @DisplayName("大小写不敏感匹配")
        void matchingShouldBeCaseInsensitive() {
            assertThat(masker.isSensitive("database.PASSWORD")).isTrue();
            assertThat(masker.isSensitive("database.Password")).isTrue();
            assertThat(masker.isSensitive("JWT.SECRET")).isTrue();
            assertThat(masker.isSensitive("API.KEY")).isTrue();
        }

        @Test
        @DisplayName("null 键不应为敏感键")
        void nullKeyShouldNotBeSensitive() {
            assertThat(masker.isSensitive(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("值脱敏 mask()")
    class MaskTests {

        private final SensitiveConfigMasker masker = SensitiveConfigMasker.defaultMasker();

        @Test
        @DisplayName("敏感键的值应被脱敏为掩码字符串")
        void sensitiveKeyValueShouldBeMasked() {
            assertThat(masker.mask("database.password", "mySecretPassword123"))
                    .isEqualTo("***");
        }

        @Test
        @DisplayName("脱敏后的输出不应包含原始明文密码")
        void maskedOutputShouldNotContainPlaintext() {
            String result = masker.mask("redis.password", "P@ssw0rd!");
            assertThat(result).doesNotContain("P@ssw0rd!");
            assertThat(result).doesNotContain("ssw0rd");
            assertThat(result).isEqualTo("***");
        }

        @Test
        @DisplayName("非敏感键的值应保持不变")
        void nonSensitiveKeyValueShouldRemainUnchanged() {
            assertThat(masker.mask("server.port", "8080"))
                    .isEqualTo("8080");
            assertThat(masker.mask("database.url", "jdbc:mysql://localhost:3306/db"))
                    .isEqualTo("jdbc:mysql://localhost:3306/db");
        }

        @Test
        @DisplayName("null 值应保持不变")
        void nullValueShouldRemainUnchanged() {
            assertThat(masker.mask("server.port", null))
                    .isNull();
        }
    }

    @Nested
    @DisplayName("批量脱敏 maskAll()")
    class MaskAllTests {

        private final SensitiveConfigMasker masker = SensitiveConfigMasker.defaultMasker();

        @Test
        @DisplayName("批量脱敏应正确处理敏感和非敏感键")
        void maskAllShouldProcessMixedEntries() {
            List<SensitiveConfigMasker.ConfigEntry> entries = List.of(
                    new SensitiveConfigMasker.ConfigEntry("server.port", "8080"),
                    new SensitiveConfigMasker.ConfigEntry("database.password", "secret123"),
                    new SensitiveConfigMasker.ConfigEntry("app.name", "my-app"),
                    new SensitiveConfigMasker.ConfigEntry("api.key", "key-abc-def")
            );

            List<SensitiveConfigMasker.ConfigEntry> result = masker.maskAll(entries);

            assertThat(result).hasSize(4);
            assertThat(result.get(0).getValue()).isEqualTo("8080");       // not sensitive
            assertThat(result.get(1).getValue()).isEqualTo("***");         // sensitive
            assertThat(result.get(2).getValue()).isEqualTo("my-app");      // not sensitive
            assertThat(result.get(3).getValue()).isEqualTo("***");         // sensitive
        }

        @Test
        @DisplayName("批量脱敏不应包含任何明文敏感值")
        void maskAllShouldNotContainPlaintext() {
            List<SensitiveConfigMasker.ConfigEntry> entries = List.of(
                    new SensitiveConfigMasker.ConfigEntry("database.password", "super-secret-pwd"),
                    new SensitiveConfigMasker.ConfigEntry("jwt.secret", "my-jwt-secret-key")
            );

            List<SensitiveConfigMasker.ConfigEntry> result = masker.maskAll(entries);

            for (SensitiveConfigMasker.ConfigEntry entry : result) {
                assertThat(entry.getValue()).doesNotContain("super-secret-pwd");
                assertThat(entry.getValue()).doesNotContain("my-jwt-secret-key");
            }
        }

        @Test
        @DisplayName("null 列表应返回空列表")
        void nullListShouldReturnEmptyList() {
            List<SensitiveConfigMasker.ConfigEntry> result = masker.maskAll(null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("自定义脱敏器")
    class CustomMaskerTests {

        @Test
        @DisplayName("自定义敏感模式和掩码值应生效")
        void customPatternsAndMaskValueShouldWork() {
            Set<String> patterns = Set.of("password", "apikey");
            SensitiveConfigMasker customMasker = new SensitiveConfigMasker(patterns, "MASKED");

            assertThat(customMasker.isSensitive("database.password")).isTrue();
            assertThat(customMasker.isSensitive("gateway.apikey")).isTrue();
            assertThat(customMasker.isSensitive("jwt.secret")).isFalse(); // not in custom patterns

            assertThat(customMasker.mask("database.password", "secret"))
                    .isEqualTo("MASKED");
        }

        @Test
        @DisplayName("null 模式集合应使用空集合")
        void nullPatternsShouldUseEmptySet() {
            SensitiveConfigMasker masker = new SensitiveConfigMasker(null, "***");

            assertThat(masker.isSensitive("database.password")).isFalse();
            assertThat(masker.mask("database.password", "secret"))
                    .isEqualTo("secret");
        }

        @Test
        @DisplayName("默认脱敏器应使用标准模式集合")
        void defaultMaskerShouldUseStandardPatterns() {
            SensitiveConfigMasker masker = SensitiveConfigMasker.defaultMasker();

            assertThat(masker.getMaskValue()).isEqualTo("***");
            assertThat(masker.getSensitiveKeyPatterns())
                    .containsExactlyInAnyOrder("password", "secret", "key", "token", "credential", "pwd");
        }
    }

    @Nested
    @DisplayName("ConfigEntry")
    class ConfigEntryTests {

        @Test
        @DisplayName("应正确创建配置键值对")
        void shouldCreateConfigEntry() {
            SensitiveConfigMasker.ConfigEntry entry =
                    new SensitiveConfigMasker.ConfigEntry("key", "value");

            assertThat(entry.getKey()).isEqualTo("key");
            assertThat(entry.getValue()).isEqualTo("value");
        }

        @Test
        @DisplayName("null key 应抛出 NullPointerException")
        void nullKeyShouldThrowException() {
            assertThatThrownBy(
                    () -> new SensitiveConfigMasker.ConfigEntry(null, "value"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("value 可为 null")
        void valueCanBeNull() {
            SensitiveConfigMasker.ConfigEntry entry =
                    new SensitiveConfigMasker.ConfigEntry("key", null);

            assertThat(entry.getValue()).isNull();
        }

        @Test
        @DisplayName("相同键值对的 ConfigEntry 应相等")
        void entriesWithSameDataShouldBeEqual() {
            SensitiveConfigMasker.ConfigEntry e1 =
                    new SensitiveConfigMasker.ConfigEntry("key", "value");
            SensitiveConfigMasker.ConfigEntry e2 =
                    new SensitiveConfigMasker.ConfigEntry("key", "value");

            assertThat(e1).isEqualTo(e2);
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }
    }
}
