package com.microservice.framework.apollo.autoconfigure;

import com.microservice.framework.apollo.ApolloProperties;
import com.microservice.framework.apollo.ConfigGovernanceProperties;
import com.microservice.framework.apollo.config.ConfigValidator;
import com.microservice.framework.apollo.config.RefreshPolicy;
import com.microservice.framework.apollo.config.SensitiveConfigMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Apollo Starter 自动配置集成测试
 * <p>
 * 使用 ApplicationContextRunner 验证自动配置的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 *
 * @author Andy Yang
 */
class ApolloAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ApolloConfigAutoConfiguration.class,
                    KubernetesConfigAutoConfiguration.class,
                    ConfigGovernanceAutoConfiguration.class))
            .withPropertyValues("framework.apollo.enabled=true");

    @Nested
    @DisplayName("默认配置")
    class DefaultConfigurationTests {

        @Test
        @DisplayName("默认配置应加载配置治理相关 Bean")
        void defaultConfigurationShouldLoadGovernanceBeans() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("apolloConfigValidator");
                assertThat(context).hasBean("apolloRefreshPolicy");
                assertThat(context).hasBean("apolloSensitiveConfigMasker");
            });
        }

        @Test
        @DisplayName("默认配置应注册 ApolloProperties Bean")
        void defaultConfigurationShouldRegisterApolloProperties() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(ApolloProperties.class)).isNotNull();
                ApolloProperties properties = context.getBean(ApolloProperties.class);
                assertThat(properties.isEnabled()).isTrue();
                assertThat(properties.getCluster()).isEqualTo("default");
                assertThat(properties.getNamespaces()).containsExactly("application");
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
        void apolloRefreshPolicyShouldBeOnChange() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                RefreshPolicy policy = context.getBean(RefreshPolicy.class);
                assertThat(policy.getStrategy()).isEqualTo(RefreshPolicy.Strategy.ON_CHANGE);
                assertThat(policy.getNonRefreshablePrefixes()).isEmpty();
            });
        }

        @Test
        @DisplayName("默认 SensitiveConfigMasker 应包含标准敏感键模式")
        void apolloSensitiveConfigMaskerShouldContainStandardPatterns() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                SensitiveConfigMasker masker = context.getBean(SensitiveConfigMasker.class);
                assertThat(masker.getMaskValue()).isEqualTo("***");
                assertThat(masker.isSensitive("database.password")).isTrue();
                assertThat(masker.isSensitive("jwt.secret")).isTrue();
                assertThat(masker.isSensitive("api.key")).isTrue();
            });
        }

        @Test
        @DisplayName("默认 appId 应为 null")
        void defaultAppIdShouldBeNull() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                ApolloProperties properties = context.getBean(ApolloProperties.class);
                assertThat(properties.getAppId()).isNull();
            });
        }
    }

    @Nested
    @DisplayName("禁用配置模块")
    class DisablingModuleTests {

        @Test
        @DisplayName("禁用 Apollo 配置中心后 ApolloProperties 仍不加载")
        void disablingApolloShouldRemoveApolloConfig() {
            contextRunner.withPropertyValues("framework.apollo.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean(ApolloProperties.class);
                    });
        }

        @Test
        @DisplayName("禁用配置治理后治理相关 Bean 不应存在")
        void disablingGovernanceShouldRemoveGovernanceBeans() {
            contextRunner.withPropertyValues("framework.config.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("apolloConfigValidator");
                        assertThat(context).doesNotHaveBean("apolloRefreshPolicy");
                        assertThat(context).doesNotHaveBean("apolloSensitiveConfigMasker");
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
                        assertThat(context).doesNotHaveBean("apolloConfigValidator");
                        assertThat(context).doesNotHaveBean(ConfigValidator.class);
                        assertThat(context).hasBean("apolloRefreshPolicy");
                        assertThat(context).hasBean("apolloSensitiveConfigMasker");
                    });
        }

        @Test
        @DisplayName("禁用刷新策略后 RefreshPolicy Bean 不应存在")
        void disablingRefreshPolicyShouldRemoveRefreshPolicyBean() {
            contextRunner.withPropertyValues("framework.config.refresh-policy.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("apolloRefreshPolicy");
                        assertThat(context).doesNotHaveBean(RefreshPolicy.class);
                        assertThat(context).hasBean("apolloConfigValidator");
                        assertThat(context).hasBean("apolloSensitiveConfigMasker");
                    });
        }

        @Test
        @DisplayName("禁用脱敏后 SensitiveConfigMasker Bean 不应存在")
        void disablingMaskingShouldRemoveMaskingBean() {
            contextRunner.withPropertyValues("framework.config.masking.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("apolloSensitiveConfigMasker");
                        assertThat(context).doesNotHaveBean(SensitiveConfigMasker.class);
                        assertThat(context).hasBean("apolloConfigValidator");
                        assertThat(context).hasBean("apolloRefreshPolicy");
                    });
        }
    }

    @Nested
    @DisplayName("属性绑定")
    class PropertyBindingTests {

        @Test
        @DisplayName("自定义 Apollo 配置应正确绑定")
        void customApolloPropertiesShouldBeBound() {
            contextRunner.withPropertyValues(
                    "framework.apollo.app-id=my-app",
                    "framework.apollo.cluster=prod-cluster",
                    "framework.apollo.namespaces[0]=application",
                    "framework.apollo.namespaces[1]=application.yml",
                    "framework.apollo.env=PRO",
                    "framework.apollo.meta-server-url=http://meta:8080")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ApolloProperties properties = context.getBean(ApolloProperties.class);
                        assertThat(properties.getAppId()).isEqualTo("my-app");
                        assertThat(properties.getCluster()).isEqualTo("prod-cluster");
                        assertThat(properties.getNamespaces()).containsExactly("application", "application.yml");
                        assertThat(properties.getEnv()).isEqualTo("PRO");
                        assertThat(properties.getMetaServerUrl()).isEqualTo("http://meta:8080");
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
                    "framework.apollo.kubernetes.enabled=true",
                    "framework.apollo.kubernetes.config-map-name=app-config",
                    "framework.apollo.kubernetes.namespace=default")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("kubernetesConfigSourceDescriptor");
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
                        assertThat(context).doesNotHaveBean("apolloConfigValidator");
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
                        assertThat(context).doesNotHaveBean("apolloRefreshPolicy");
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
                        assertThat(context).doesNotHaveBean("apolloSensitiveConfigMasker");
                        assertThat(context.getBean(SensitiveConfigMasker.class)).isEqualTo(customMasker);
                    });
        }
    }
}
