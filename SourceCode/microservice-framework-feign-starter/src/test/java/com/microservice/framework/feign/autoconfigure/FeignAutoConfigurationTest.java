package com.microservice.framework.feign.autoconfigure;

import com.microservice.framework.common.autoconfigure.CommonContextAutoConfiguration;
import com.microservice.framework.common.autoconfigure.CommonIdAutoConfiguration;
import com.microservice.framework.common.autoconfigure.CommonTimeAutoConfiguration;
import com.microservice.framework.feign.api.FeignContextPropagator;
import com.microservice.framework.feign.api.ServiceIdentityProvider;
import feign.RequestInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feign 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证 FeignAutoConfiguration 的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 *
 * @author Andy Yang
 */
class FeignAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CommonTimeAutoConfiguration.class,
                    CommonIdAutoConfiguration.class,
                    CommonContextAutoConfiguration.class,
                    FeignAutoConfiguration.class))
            .withPropertyValues("framework.common.id.worker-id=1");

    // ======================================================================
     // 默认激活
     // ======================================================================

    @Nested
    @DisplayName("默认配置激活")
    class DefaultActivation {

        @Test
        @DisplayName("默认配置应激活所有 Feign Bean")
        void defaultConfigurationShouldActivateAllFeignBeans() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("feignContextPropagator");
                assertThat(context).hasBean("serviceIdentityProvider");
                assertThat(context).hasBean("feignContextPropagationInterceptor");
            });
        }

        @Test
        @DisplayName("默认 FeignContextPropagator 应为 FeignAutoConfiguration 内部实现")
        void defaultPropagatorShouldBeInternalImplementation() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                FeignContextPropagator propagator = context.getBean(FeignContextPropagator.class);
                assertThat(propagator).isInstanceOf(FeignAutoConfiguration.DefaultFeignContextPropagator.class);
            });
        }

        @Test
        @DisplayName("默认 ServiceIdentityProvider 应为 FeignAutoConfiguration 内部实现")
        void defaultIdentityProviderShouldBeInternalImplementation() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                ServiceIdentityProvider provider = context.getBean(ServiceIdentityProvider.class);
                assertThat(provider).isInstanceOf(FeignAutoConfiguration.DefaultServiceIdentityProvider.class);
            });
        }
    }

    // ======================================================================
     // 禁用条件
     // ======================================================================

    @Nested
    @DisplayName("禁用条件")
    class DisablingConditions {

        @Test
        @DisplayName("framework.feign.enabled=false 应禁用所有 Feign Bean")
        void disablingFeignShouldRemoveAllBeans() {
            contextRunner.withPropertyValues("framework.feign.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("feignContextPropagator");
                        assertThat(context).doesNotHaveBean("serviceIdentityProvider");
                        assertThat(context).doesNotHaveBean("feignContextPropagationInterceptor");
                        assertThat(context).doesNotHaveBean(FeignContextPropagator.class);
                        assertThat(context).doesNotHaveBean(ServiceIdentityProvider.class);
                        assertThat(context).doesNotHaveBean(RequestInterceptor.class);
                    });
        }

        @Test
        @DisplayName("禁用上下文传播后拦截器不应注入上下文头")
        void disablingContextPropagationShouldStillActivateInterceptor() {
            contextRunner.withPropertyValues("framework.feign.context.propagation-enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("feignContextPropagationInterceptor");
                    });
        }

        @Test
        @DisplayName("禁用服务身份后 ServiceIdentityProvider Bean 不应存在")
        void disablingServiceIdentityShouldRemoveIdentityProvider() {
            contextRunner.withPropertyValues("framework.feign.service-identity.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean("serviceIdentityProvider");
                        assertThat(context).doesNotHaveBean(ServiceIdentityProvider.class);
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
        @DisplayName("配置 serviceName 应注入到 DefaultServiceIdentityProvider")
        void configuredServiceNameShouldBeAppliedToProvider() {
            contextRunner.withPropertyValues("framework.feign.service-identity.service-name=order-service")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        ServiceIdentityProvider provider = context.getBean(ServiceIdentityProvider.class);
                        assertThat(provider.getServiceId()).isEqualTo("order-service");
                    });
        }

        @Test
        @DisplayName("未配置 serviceName 时默认应为 unknown")
        void unconfiguredServiceNameShouldDefaultToUnknown() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                ServiceIdentityProvider provider = context.getBean(ServiceIdentityProvider.class);
                assertThat(provider.getServiceId()).isEqualTo("unknown");
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
        @DisplayName("用户提供的 FeignContextPropagator 应覆盖默认实现")
        void userProvidedPropagatorShouldOverrideDefault() {
            FeignContextPropagator customPropagator = new FeignContextPropagator() {
                @Override
                public java.util.Map<String, String> propagate(
                        com.microservice.framework.common.context.ContextSnapshot snapshot) {
                    return java.util.Map.of("X-Custom", "custom-value");
                }

                @Override
                public void restore(java.util.Map<String, String> headers) {
                }
            };

            contextRunner.withBean("customFeignContextPropagator", FeignContextPropagator.class,
                    () -> customPropagator)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customFeignContextPropagator");
                        assertThat(context).doesNotHaveBean("feignContextPropagator");
                        assertThat(context.getBean(FeignContextPropagator.class))
                                .isSameAs(customPropagator);
                    });
        }

        @Test
        @DisplayName("用户提供的 ServiceIdentityProvider 应覆盖默认实现")
        void userProvidedIdentityProviderShouldOverrideDefault() {
            ServiceIdentityProvider customProvider = new ServiceIdentityProvider() {
                @Override
                public String getServiceId() {
                    return "custom-service";
                }

                @Override
                public String getServiceToken() {
                    return "custom-token";
                }

                @Override
                public java.util.Map<String, String> getHeaders() {
                    return java.util.Map.of("X-Custom-Auth", "bearer-token");
                }
            };

            contextRunner.withBean("customServiceIdentityProvider", ServiceIdentityProvider.class,
                    () -> customProvider)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customServiceIdentityProvider");
                        assertThat(context).doesNotHaveBean("serviceIdentityProvider");
                        assertThat(context.getBean(ServiceIdentityProvider.class))
                                .isSameAs(customProvider);
                    });
        }
    }
}
