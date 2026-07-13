package com.microservice.framework.feign.autoconfigure;

import com.microservice.framework.common.autoconfigure.CommonContextAutoConfiguration;
import com.microservice.framework.common.autoconfigure.CommonIdAutoConfiguration;
import com.microservice.framework.common.autoconfigure.CommonTimeAutoConfiguration;
import com.microservice.framework.feign.api.FeignContextPropagator;
import com.microservice.framework.feign.api.ServiceIdentityProvider;
import feign.RequestTemplate;
import feign.RequestInterceptor;
import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        @DisplayName("连接超时属性必须配置到 Feign Request.Options")
        void connectionTimeoutPropertiesShouldConfigureFeignOptions() {
            contextRunner.withPropertyValues(
                            "framework.feign.connection.connect-timeout=321",
                            "framework.feign.connection.read-timeout=654")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        Request.Options options = context.getBean(Request.Options.class);
                        assertThat(options.connectTimeoutMillis()).isEqualTo(321);
                        assertThat(options.readTimeoutMillis()).isEqualTo(654);
                    });
        }

        @Test
        @DisplayName("重试属性必须配置为 Feign Retryer")
        void retryPropertiesShouldConfigureFeignRetryer() {
            contextRunner.withPropertyValues(
                            "framework.feign.retry.enabled=true",
                            "framework.feign.retry.max-retries=2",
                            "framework.feign.retry.retry-interval=17")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBean(Retryer.class))
                                .isInstanceOf(FeignAutoConfiguration.BudgetAwareRetryer.class);
                    });
        }

        @Test
        @DisplayName("禁用重试时必须安装 NEVER_RETRY")
        void disabledRetryShouldInstallNeverRetry() {
            contextRunner.withPropertyValues("framework.feign.retry.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBean(Retryer.class)).isSameAs(Retryer.NEVER_RETRY);
                    });
        }

        @Test
        @DisplayName("读取超时不得超过总请求预算")
        void readTimeoutShouldNotExceedTotalRequestBudget() {
            contextRunner.withPropertyValues(
                            "framework.feign.connection.timeout=250",
                            "framework.feign.connection.connect-timeout=100",
                            "framework.feign.connection.read-timeout=1000")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        Request.Options options = context.getBean(Request.Options.class);
                        assertThat(options.readTimeoutMillis()).isEqualTo(250);
                    });
        }

        @Test
        @DisplayName("重试器必须拒绝非幂等 HTTP 方法")
        void retryerShouldRejectNonIdempotentMethods() {
            contextRunner.withPropertyValues(
                            "framework.feign.retry.enabled=true",
                            "framework.feign.retry.max-retries=3",
                            "framework.feign.retry.retry-interval=0")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        Retryer retryer = context.getBean(Retryer.class).clone();
                        RetryableException postFailure = retryable(Request.HttpMethod.POST);

                        assertThatThrownBy(() -> retryer.continueOrPropagate(postFailure))
                                .isSameAs(postFailure);
                    });
        }

        @Test
        @DisplayName("重试器必须用总请求预算限制后续重试")
        void retryerShouldEnforceTotalRequestBudget() {
            contextRunner.withPropertyValues(
                            "framework.feign.connection.timeout=1",
                            "framework.feign.retry.enabled=true",
                            "framework.feign.retry.max-retries=3",
                            "framework.feign.retry.retry-interval=50")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        Retryer retryer = context.getBean(Retryer.class).clone();
                        RetryableException getFailure = retryable(Request.HttpMethod.GET);

                        assertThatThrownBy(() -> retryer.continueOrPropagate(getFailure))
                                .isSameAs(getFailure);
                    });
        }

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

        @Test
        @DisplayName("默认服务身份提供者不得返回空 Token 凭据")
        void defaultServiceIdentityProviderShouldNotExposeEmptyTokenCredential() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                ServiceIdentityProvider provider = context.getBean(ServiceIdentityProvider.class);
                assertThat(provider.getServiceToken()).isNull();
            });
        }

        @Test
        @DisplayName("拦截器不得注入空白服务身份凭据头")
        void interceptorShouldNotEmitBlankServiceIdentityCredentials() {
            ServiceIdentityProvider blankProvider = new ServiceIdentityProvider() {
                @Override
                public String getServiceId() {
                    return " ";
                }

                @Override
                public String getServiceToken() {
                    return " ";
                }

                @Override
                public Map<String, String> getHeaders() {
                    return Map.of("X-Service-Token", " ", "X-Custom-Credential", "");
                }
            };

            contextRunner.withBean(ServiceIdentityProvider.class, () -> blankProvider)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        RequestInterceptor interceptor = context.getBean(RequestInterceptor.class);
                        RequestTemplate template = new RequestTemplate();

                        interceptor.apply(template);

                        assertThat(template.headers())
                                .doesNotContainKey("X-Service-Token")
                                .doesNotContainKey("X-Custom-Credential")
                                .doesNotContainKey("serviceIdentity");
                    });
        }
    }

    @Nested
    @DisplayName("配置声明无法兑现时快速失败")
    class UnsupportedConfiguredClaims {

        @Test
        @DisplayName("启用熔断但未提供实现时必须启动失败")
        void enabledCircuitBreakerShouldFailFastWithoutImplementation() {
            contextRunner.withPropertyValues("framework.feign.circuit-breaker.enabled=true")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.feign.circuit-breaker.enabled=true");
                    });
        }

        @Test
        @DisplayName("启用隔离但未提供实现时必须启动失败")
        void enabledIsolationShouldFailFastWithoutImplementation() {
            contextRunner.withPropertyValues("framework.feign.isolation.enabled=true")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.feign.isolation.enabled=true");
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

    private static RetryableException retryable(Request.HttpMethod method) {
        Request request = Request.create(method, "http://downstream.test",
                Map.<String, Collection<String>>of(), (byte[]) null, (Charset) null);
        return new RetryableException(503, "unavailable", method, (Long) null, request);
    }
}
