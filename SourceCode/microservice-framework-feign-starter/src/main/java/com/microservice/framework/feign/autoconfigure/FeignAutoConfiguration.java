package com.microservice.framework.feign.autoconfigure;

import com.microservice.framework.common.context.ContextKeys;
import com.microservice.framework.common.context.ContextSnapshot;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import com.microservice.framework.feign.FeignProperties;
import com.microservice.framework.feign.api.FeignContextPropagator;
import com.microservice.framework.feign.api.ServiceIdentityProvider;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

/**
 * Feign 自动配置
 * <p>
 * 当 Spring Cloud OpenFeign 在类路径上且 {@code framework.feign} 未被显式禁用时激活。
 * 注册以下核心 Bean：
 * <ul>
 *   <li>{@link FeignContextPropagator} — 上下文传播实现</li>
 *   <li>{@link ServiceIdentityProvider} — 服务身份提供实现</li>
 *   <li>{@link RequestInterceptor} — Feign 请求拦截器，自动注入上下文和身份信息</li>
 * </ul>
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(FeignProperties.class)
@ConditionalOnClass(name = "feign.RequestInterceptor")
@ConditionalOnProperty(prefix = "framework.feign", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class FeignAutoConfiguration {

    /**
     * 上下文传播实现
     * <p>
     * 从 {@link ThreadLocalContextAdapter} 的快照中提取配置的传播键，
     * 并将其作为 Feign 请求头传递。
     *
     * @param properties Feign 配置属性
     * @return {@link FeignContextPropagator} 实现
     */
    @Bean
    @ConditionalOnMissingBean(FeignContextPropagator.class)
    public FeignContextPropagator feignContextPropagator(FeignProperties properties) {
        return new DefaultFeignContextPropagator(properties.getContext().getPropagateKeys());
    }

    /**
     * 服务身份提供实现
     * <p>
     * 使用配置的 {@code framework.feign.service-identity.service-name} 作为服务标识，
     * 生成静态的服务身份信息。用户可通过注册自定义 {@link ServiceIdentityProvider} Bean
     * 覆盖此默认实现。
     *
     * @param properties Feign 配置属性
     * @return {@link ServiceIdentityProvider} 实现
     */
    @Bean
    @ConditionalOnMissingBean(ServiceIdentityProvider.class)
    @ConditionalOnProperty(prefix = "framework.feign.service-identity", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public ServiceIdentityProvider serviceIdentityProvider(FeignProperties properties) {
        return new DefaultServiceIdentityProvider(
                properties.getServiceIdentity().getServiceName());
    }

    /**
     * Feign 请求拦截器 — 自动注入上下文和服务身份信息
     * <p>
     * 优先注入上下文传播键（requestId, traceId, userId 等），
     * 然后注入服务身份信息（serviceId, serviceToken 及附加头）。
     *
     * @param contextAdapter       ThreadLocal 上下文适配器
     * @param propagator           上下文传播器
     * @param identityProvider     服务身份提供者（可选，可能因条件未满足而不存在）
     * @param properties           Feign 配置属性
     * @return Feign {@link RequestInterceptor}
     */
    @Bean
    @ConditionalOnMissingBean(name = "feignContextPropagationInterceptor")
    public RequestInterceptor feignContextPropagationInterceptor(
            ThreadLocalContextAdapter contextAdapter,
            FeignContextPropagator propagator,
            ObjectProvider<ServiceIdentityProvider> identityProvider,
            FeignProperties properties) {
        return new FeignContextPropagationInterceptor(
                contextAdapter, propagator, identityProvider, properties);
    }

    // ======================================================================
     // 内部实现类
     // ======================================================================

    /**
     * 默认上下文传播实现
     */
    static class DefaultFeignContextPropagator implements FeignContextPropagator {

        private final java.util.Set<String> propagateKeys;

        DefaultFeignContextPropagator(java.util.Set<String> propagateKeys) {
            this.propagateKeys = propagateKeys != null
                    ? java.util.Collections.unmodifiableSet(new java.util.HashSet<>(propagateKeys))
                    : java.util.Collections.emptySet();
        }

        @Override
        public Map<String, String> propagate(ContextSnapshot snapshot) {
            if (snapshot == null) {
                return java.util.Collections.emptyMap();
            }
            Map<String, String> headers = new HashMap<>();
            for (String key : propagateKeys) {
                String value = snapshot.get(key);
                if (value != null) {
                    headers.put(key, value);
                }
            }
            return headers;
        }

        @Override
        public void restore(Map<String, String> headers) {
            // restore is primarily used for incoming response processing;
            // the Feign outgoing path only propagates context into request headers
        }
    }

    /**
     * 默认服务身份提供实现
     */
    static class DefaultServiceIdentityProvider implements ServiceIdentityProvider {

        private final String serviceName;

        DefaultServiceIdentityProvider(String serviceName) {
            this.serviceName = serviceName != null ? serviceName : "unknown";
        }

        @Override
        public String getServiceId() {
            return serviceName;
        }

        @Override
        public String getServiceToken() {
            // Default implementation returns empty token;
            // users should provide their own ServiceIdentityProvider bean
            // with a real authentication mechanism
            return "";
        }

        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put(ContextKeys.SERVICE_IDENTITY, serviceName);
            return headers;
        }
    }

    /**
     * Feign 请求拦截器 — 将上下文和服务身份注入到每个 Feign 请求
     */
    static class FeignContextPropagationInterceptor implements RequestInterceptor {

        private final ThreadLocalContextAdapter contextAdapter;
        private final FeignContextPropagator propagator;
        private final ObjectProvider<ServiceIdentityProvider> identityProvider;
        private final FeignProperties properties;

        FeignContextPropagationInterceptor(ThreadLocalContextAdapter contextAdapter,
                                           FeignContextPropagator propagator,
                                           ObjectProvider<ServiceIdentityProvider> identityProvider,
                                           FeignProperties properties) {
            this.contextAdapter = contextAdapter;
            this.propagator = propagator;
            this.identityProvider = identityProvider;
            this.properties = properties;
        }

        @Override
        public void apply(RequestTemplate template) {
            // Propagate context keys if enabled
            if (properties.getContext().isPropagationEnabled()) {
                ContextSnapshot snapshot = contextAdapter.snapshot();
                Map<String, String> contextHeaders = propagator.propagate(snapshot);
                contextHeaders.forEach(template::header);
            }

            // Inject service identity headers if enabled and provider is available
            if (properties.getServiceIdentity().isEnabled()) {
                ServiceIdentityProvider provider = identityProvider.getIfAvailable();
                if (provider != null) {
                    template.header(ContextKeys.SERVICE_IDENTITY, provider.getServiceId());
                    String token = provider.getServiceToken();
                    if (token != null && !token.isEmpty()) {
                        template.header("X-Service-Token", token);
                    }
                    provider.getHeaders().forEach(template::header);
                }
            }
        }
    }
}
