package com.microservice.framework.nacos.autoconfigure;

import com.microservice.framework.nacos.ConfigGovernanceProperties;
import com.microservice.framework.nacos.NacosProperties;
import com.microservice.framework.nacos.config.ConfigValidator;
import com.microservice.framework.nacos.config.RefreshPolicy;
import com.microservice.framework.nacos.config.SensitiveConfigMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nacos Starter 自动配置集成测试
 * <p>
 * 使用 ApplicationContextRunner 验证自动配置的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 *
 * @author Andy Yang
 */
class NacosAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    NacosConfigAutoConfiguration.class,
                    KubernetesConfigAutoConfiguration.class,
                    ConfigGovernanceAutoConfiguration.class,
                    ConfigCenterMutualExclusionAutoConfiguration.class))
            .withPropertyValues("framework.nacos.enabled=true");

    @Nested
    @DisplayName("默认配置")
    class DefaultConfigurationTests {

        @Test
        @DisplayName("仅引入 Nacos Starter 时不应被判定为配置中心冲突")
        void nacosOnlyShouldNotBeTreatedAsConfigCenterConflict() {
            contextRunner.run(context -> assertThat(context).hasNotFailed());
        }

        @Test
        @DisplayName("默认配置应加载配置治理相关 Bean")
        void defaultConfigurationShouldLoadGovernanceBeans() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("nacosConfigValidator");
                assertThat(context).hasBean("nacosRefreshPolicy");
                assertThat(context).hasBean("nacosSensitiveConfigMasker");
            });
        }

        @Test
        @DisplayName("默认配置应注册 NacosProperties Bean")
        void defaultConfigurationShouldRegisterNacosProperties() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(NacosProperties.class)).isNotNull();
                NacosProperties properties = context.getBean(NacosProperties.class);
                assertThat(properties.isEnabled()).isTrue();
                assertThat(properties.getServerAddr()).isEqualTo("localhost:8848");
                assertThat(properties.getGroup()).isEqualTo("DEFAULT_GROUP");
            });
        }

        @Test
        @DisplayName("默认配置应注册 ConfigGovernanceProperties Bean")
        void defaultConfigurationShouldRegisterConfigGovernanceProperties() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(ConfigGovernanceProperties.class)).isNotNull();
                ConfigGovernanceProperties properties = context.getBean(ConfigGovernanceProperties.class);
                assertThat(properties.isEnabled()).isTrue();
                assertThat(properties.getValidator().isEnabled()).isTrue();
                assertThat(properties.getRefreshPolicy().isEnabled()).isTrue();
                assertThat(properties.getMasking().isEnabled()).isTrue();
            });
        }

        @Test
        @DisplayName("默认 RefreshPolicy 应为 ON_CHANGE 策略")
        void nacosRefreshPolicyShouldBeOnChange() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                RefreshPolicy policy = context.getBean(RefreshPolicy.class);
                assertThat(policy.getStrategy()).isEqualTo(RefreshPolicy.Strategy.ON_CHANGE);
                assertThat(policy.getNonRefreshablePrefixes()).isEmpty();
            });
        }

        @Test
        @DisplayName("默认 SensitiveConfigMasker 应包含标准敏感键模式")
        void nacosSensitiveConfigMaskerShouldContainStandardPatterns() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                SensitiveConfigMasker masker = context.getBean(SensitiveConfigMasker.class);
                assertThat(masker.getMaskValue()).isEqualTo("***");
                assertThat(masker.isSensitive("database.password")).isTrue();
                assertThat(masker.isSensitive("jwt.secret")).isTrue();
                assertThat(masker.isSensitive("api.key")).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("禁用配置模块")
    class DisablingModuleTests {

        @Test
        @DisplayName("禁用 Nacos 配置中心后 NacosProperties 仍不加载")
        void disablingNacosShouldRemoveNacosConfig() {
            contextRunner.withPropertyValues("framework.nacos.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean(NacosProperties.class);
                    });
        }

        @Test
        @DisplayName("禁用配置治理后治理相关 Bean 不应存在")
        void disablingGovernanceShouldRemoveGovernanceBeans() {
            contextRunner.withPropertyValues("framework.config.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("nacosConfigValidator");
                        assertThat(context).doesNotHaveBean("nacosRefreshPolicy");
                        assertThat(context).doesNotHaveBean("nacosSensitiveConfigMasker");
                        assertThat(context).doesNotHaveBean(ConfigValidator.class);
                        assertThat(context).doesNotHaveBean(RefreshPolicy.class);
                        assertThat(context).doesNotHaveBean(SensitiveConfigMasker.class);
                    });
        }

        @Test
        @DisplayName("禁用配置校验后 ConfigValidator Bean 不应存在")
        void disablingValidatorShouldRemoveValidatorBean() {
            contextRunner.withPropertyValues("framework.config.validator.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("nacosConfigValidator");
                        assertThat(context).doesNotHaveBean(ConfigValidator.class);
                        assertThat(context).hasBean("nacosRefreshPolicy");
                        assertThat(context).hasBean("nacosSensitiveConfigMasker");
                    });
        }

        @Test
        @DisplayName("禁用刷新策略后 RefreshPolicy Bean 不应存在")
        void disablingRefreshPolicyShouldRemoveRefreshPolicyBean() {
            contextRunner.withPropertyValues("framework.config.refresh-policy.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("nacosRefreshPolicy");
                        assertThat(context).doesNotHaveBean(RefreshPolicy.class);
                        assertThat(context).hasBean("nacosConfigValidator");
                        assertThat(context).hasBean("nacosSensitiveConfigMasker");
                    });
        }

        @Test
        @DisplayName("禁用脱敏后 SensitiveConfigMasker Bean 不应存在")
        void disablingMaskingShouldRemoveMaskingBean() {
            contextRunner.withPropertyValues("framework.config.masking.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("nacosSensitiveConfigMasker");
                        assertThat(context).doesNotHaveBean(SensitiveConfigMasker.class);
                        assertThat(context).hasBean("nacosConfigValidator");
                        assertThat(context).hasBean("nacosRefreshPolicy");
                    });
        }
    }

    @Nested
    @DisplayName("Kubernetes 配置源")
    class KubernetesConfigTests {

        @Test
        @DisplayName("默认不启用 Kubernetes 配置源")
        void kubernetesConfigShouldNotBeEnabledByDefault() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean("kubernetesConfigSourceDescriptor");
            });
        }

        @Test
        @DisplayName("启用 Kubernetes 配置源后应注册配置源描述符")
        void enablingKubernetesShouldRegisterConfigSourceDescriptor() {
            contextRunner.withPropertyValues(
                    "framework.nacos.kubernetes.enabled=true",
                    "framework.nacos.kubernetes.config-map-name=app-config",
                    "framework.nacos.kubernetes.namespace=default")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("kubernetesConfigSourceDescriptor");
                    });
        }
    }

    @Nested
    @DisplayName("属性绑定")
    class PropertyBindingTests {

        @Test
        @DisplayName("自定义 Nacos 配置应正确绑定")
        void customNacosPropertiesShouldBeBound() {
            contextRunner.withPropertyValues(
                    "framework.nacos.server-addr=nacos.prod:8848",
                    "framework.nacos.namespace=production",
                    "framework.nacos.group=CUSTOM_GROUP",
                    "framework.nacos.data-id=app-prod.yml",
                    "framework.nacos.timeout=5000",
                    "framework.nacos.max-retry=5")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        NacosProperties properties = context.getBean(NacosProperties.class);
                        assertThat(properties.getServerAddr()).isEqualTo("nacos.prod:8848");
                        assertThat(properties.getNamespace()).isEqualTo("production");
                        assertThat(properties.getGroup()).isEqualTo("CUSTOM_GROUP");
                        assertThat(properties.getDataId()).isEqualTo("app-prod.yml");
                        assertThat(properties.getTimeout()).isEqualTo(5000);
                        assertThat(properties.getMaxRetry()).isEqualTo(5);
                    });
        }

        @Test
        @DisplayName("服务发现默认不启用")
        void serviceDiscoveryShouldNotBeEnabledByDefault() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                NacosProperties properties = context.getBean(NacosProperties.class);
                assertThat(properties.isServiceDiscoveryEnabled()).isFalse();
            });
        }

        @Test
        @DisplayName("自定义配置治理属性应正确绑定")
        void customGovernancePropertiesShouldBeBound() {
            contextRunner.withPropertyValues(
                    "framework.config.refresh-policy.default-strategy=MANUAL",
                    "framework.config.refresh-policy.non-refreshable-prefixes[0]=spring.datasource.",
                    "framework.config.masking.mask-value=---",
                    "framework.config.masking.sensitive-key-patterns[0]=password",
                    "framework.config.masking.sensitive-key-patterns[1]=secret")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ConfigGovernanceProperties properties = context.getBean(ConfigGovernanceProperties.class);
                        assertThat(properties.getRefreshPolicy().getDefaultStrategy()).isEqualTo("MANUAL");
                        assertThat(properties.getRefreshPolicy().getNonRefreshablePrefixes())
                                .containsExactly("spring.datasource.");
                        assertThat(properties.getMasking().getMaskValue()).isEqualTo("---");
                    });
        }
    }

    @Nested
    @DisplayName("用户自定义 Bean 覆盖")
    class UserOverrideTests {

        @Test
        @DisplayName("用户提供的 ConfigValidator 应覆盖默认 Bean")
        void userProvidedConfigValidatorShouldOverrideDefault() {
            ConfigValidator customValidator = ConfigValidator.pattern("^[a-z]+$");
            contextRunner.withBean("customConfigValidator", ConfigValidator.class, () -> customValidator)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customConfigValidator");
                        assertThat(context).doesNotHaveBean("nacosConfigValidator");
                        assertThat(context.getBean(ConfigValidator.class)).isEqualTo(customValidator);
                    });
        }

        @Test
        @DisplayName("用户提供的 RefreshPolicy 应覆盖默认 Bean")
        void userProvidedRefreshPolicyShouldOverrideDefault() {
            RefreshPolicy customPolicy = RefreshPolicy.none();
            contextRunner.withBean("customRefreshPolicy", RefreshPolicy.class, () -> customPolicy)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customRefreshPolicy");
                        assertThat(context).doesNotHaveBean("nacosRefreshPolicy");
                        assertThat(context.getBean(RefreshPolicy.class)).isEqualTo(customPolicy);
                    });
        }

        @Test
        @DisplayName("用户提供的 SensitiveConfigMasker 应覆盖默认 Bean")
        void userProvidedSensitiveConfigMaskerShouldOverrideDefault() {
            SensitiveConfigMasker customMasker = new SensitiveConfigMasker(
                    java.util.Set.of("password"), "MASKED");
            contextRunner.withBean("customSensitiveConfigMasker", SensitiveConfigMasker.class, () -> customMasker)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customSensitiveConfigMasker");
                        assertThat(context).doesNotHaveBean("nacosSensitiveConfigMasker");
                        assertThat(context.getBean(SensitiveConfigMasker.class)).isEqualTo(customMasker);
                    });
        }
    }
}
