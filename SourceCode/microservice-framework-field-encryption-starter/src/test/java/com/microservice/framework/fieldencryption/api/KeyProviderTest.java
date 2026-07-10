package com.microservice.framework.fieldencryption.api;

import com.microservice.framework.fieldencryption.FieldEncryptionProperties;
import com.microservice.framework.fieldencryption.autoconfigure.FieldEncryptionAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KeyProvider 接口测试
 * <p>
 * 通过 ApplicationContextRunner 验证 KeyProvider 接口的行为，
 * 确保默认实现满足密钥生命周期管理的接口契约。
 *
 * @author Andy Yang
 */
class KeyProviderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FieldEncryptionAutoConfiguration.class));

    @Test
    @DisplayName("getActiveKey 应返回活跃密钥")
    void getActiveKeyShouldReturnActiveKey() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            KeyProvider provider = context.getBean(KeyProvider.class);
            EncryptionKey activeKey = provider.getActiveKey();

            assertThat(activeKey).isNotNull();
            assertThat(activeKey.isActive()).isTrue();
            assertThat(activeKey.getKeyId()).startsWith("key-");
            assertThat(activeKey.getAlgorithm()).isEqualTo("AES/GCM/NoPadding");
        });
    }

    @Test
    @DisplayName("getKey 应通过 keyId 查找密钥")
    void getKeyShouldFindKeyById() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            KeyProvider provider = context.getBean(KeyProvider.class);
            EncryptionKey activeKey = provider.getActiveKey();

            java.util.Optional<EncryptionKey> found = provider.getKey(activeKey.getKeyId());
            assertThat(found).isPresent();
            assertThat(found.get().getKeyId()).isEqualTo(activeKey.getKeyId());
        });
    }

    @Test
    @DisplayName("getKey 对不存在的 keyId 应返回空 Optional")
    void getKeyShouldReturnEmptyForNonexistentKeyId() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            KeyProvider provider = context.getBean(KeyProvider.class);
            java.util.Optional<EncryptionKey> found = provider.getKey("nonexistent-key");
            assertThat(found).isEmpty();
        });
    }

    @Test
    @DisplayName("rotateKey 应生成新活跃密钥并停用旧密钥")
    void rotateKeyShouldGenerateNewActiveKeyAndDeactivateOld() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            KeyProvider provider = context.getBean(KeyProvider.class);

            EncryptionKey oldActive = provider.getActiveKey();
            EncryptionKey newActive = provider.rotateKey();

            assertThat(newActive).isNotNull();
            assertThat(newActive.isActive()).isTrue();
            assertThat(newActive.getKeyId()).isNotEqualTo(oldActive.getKeyId());

            // Old key should be deactivated
            java.util.Optional<EncryptionKey> oldKey = provider.getKey(oldActive.getKeyId());
            assertThat(oldKey).isPresent();
            assertThat(oldKey.get().isActive()).isFalse();
        });
    }

    @Test
    @DisplayName("deactivateKey 应将密钥标记为非活跃")
    void deactivateKeyShouldMarkKeyAsInactive() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            KeyProvider provider = context.getBean(KeyProvider.class);

            EncryptionKey activeKey = provider.getActiveKey();
            provider.deactivateKey(activeKey.getKeyId());

            // After deactivation, the key should still exist but be inactive
            java.util.Optional<EncryptionKey> key = provider.getKey(activeKey.getKeyId());
            assertThat(key).isPresent();
            assertThat(key.get().isActive()).isFalse();
        });
    }

    @Test
    @DisplayName("密钥应包含过期时间")
    void keyShouldContainExpiresAtTime() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            KeyProvider provider = context.getBean(KeyProvider.class);
            EncryptionKey activeKey = provider.getActiveKey();

            assertThat(activeKey.getExpiresAt()).isPresent();
            assertThat(activeKey.getCreatedAt()).isBefore(activeKey.getExpiresAt().orElse(Instant.MAX));
        });
    }
}
