package com.microservice.framework.feign.autoconfigure;

import com.microservice.framework.common.context.ContextKeys;
import com.microservice.framework.common.context.ContextSnapshot;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import com.microservice.framework.feign.FeignProperties;
import com.microservice.framework.feign.api.FeignContextPropagator;
import com.microservice.framework.feign.api.ServiceIdentityProvider;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.RetryableException;
import feign.Retryer;
import feign.Client;
import feign.Response;
import feign.codec.ErrorDecoder;
import feign.httpclient.ApacheHttpClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

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

    @Bean
    @ConditionalOnMissingBean(PoolingHttpClientConnectionManager.class)
    public PoolingHttpClientConnectionManager feignConnectionManager(FeignProperties properties) {
        var connection = properties.getConnection();
        var manager = new PoolingHttpClientConnectionManager(
                Math.max(1, connection.getConnectionTimeToLive()), TimeUnit.MILLISECONDS);
        manager.setMaxTotal(connection.getMaxConnections());
        manager.setDefaultMaxPerRoute(connection.getMaxConnectionsPerRoute());
        return manager;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(CloseableHttpClient.class)
    public CloseableHttpClient feignApacheHttpClient(PoolingHttpClientConnectionManager connectionManager) {
        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }

    @Bean
    @ConditionalOnMissingBean(Client.class)
    public Client feignClient(CloseableHttpClient httpClient, ObjectProvider<MeterRegistry> meterRegistry) {
        return new MeteredFeignClient(new ApacheHttpClient(httpClient), meterRegistry.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    public ErrorDecoder feignErrorDecoder(FeignProperties properties) {
        return new RetryableStatusErrorDecoder(properties.getRetry().getRetryOnStatuses());
    }

    @Bean
    @ConditionalOnMissingBean(Request.Options.class)
    public Request.Options feignRequestOptions(FeignProperties properties) {
        FeignProperties.ConnectionProperties connection = properties.getConnection();
        int totalBudget = connection.getTimeout();
        int connectTimeout = boundedTimeout(connection.getConnectTimeout(), totalBudget);
        int readTimeout = boundedTimeout(connection.getReadTimeout(), totalBudget);
        return new Request.Options(
                connectTimeout, TimeUnit.MILLISECONDS,
                readTimeout, TimeUnit.MILLISECONDS,
                true);
    }

    @Bean
    @ConditionalOnMissingBean(Retryer.class)
    public Retryer feignRetryer(FeignProperties properties) {
        FeignProperties.RetryProperties retry = properties.getRetry();
        if (!retry.isEnabled()) {
            return Retryer.NEVER_RETRY;
        }
        long interval = retry.getRetryInterval();
        return new BudgetAwareRetryer(
                retry.getMaxRetries(),
                interval,
                properties.getConnection().getTimeout(),
                retry.getIdempotentMethods());
    }

    @Bean
    public SmartInitializingSingleton feignConfiguredClaimsValidator(FeignProperties properties) {
        return () -> {
            if (properties.getCircuitBreaker().isEnabled()) {
                throw new IllegalStateException(
                        "framework.feign.circuit-breaker.enabled=true cannot be honored by this starter: "
                                + "no circuit-breaker implementation is auto-configured");
            }
            if (properties.getIsolation().isEnabled()) {
                throw new IllegalStateException(
                        "framework.feign.isolation.enabled=true cannot be honored by this starter: "
                                + "no isolation implementation is auto-configured");
            }
        };
    }

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
            return null;
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
                    addHeaderIfPresent(template, ContextKeys.SERVICE_IDENTITY, provider.getServiceId());
                    String token = provider.getServiceToken();
                    addHeaderIfPresent(template, "X-Service-Token", token);
                    Map<String, String> headers = provider.getHeaders();
                    if (headers != null) {
                        headers.forEach((name, value) -> addHeaderIfPresent(template, name, value));
                    }
                }
            }
        }
    }

    static class BudgetAwareRetryer implements Retryer {

        private final int maxRetries;
        private final long intervalMillis;
        private final long totalBudgetMillis;
        private final Set<String> idempotentMethods;
        private final long startedAtNanos;
        private int retriesAttempted;

        BudgetAwareRetryer(int maxRetries, long intervalMillis, long totalBudgetMillis,
                           Set<String> idempotentMethods) {
            this(maxRetries, intervalMillis, totalBudgetMillis, idempotentMethods, System.nanoTime());
        }

        private BudgetAwareRetryer(int maxRetries, long intervalMillis, long totalBudgetMillis,
                                   Set<String> idempotentMethods, long startedAtNanos) {
            this.maxRetries = Math.max(0, maxRetries);
            this.intervalMillis = Math.max(0, intervalMillis);
            this.totalBudgetMillis = Math.max(0, totalBudgetMillis);
            this.idempotentMethods = normalizeMethods(idempotentMethods);
            this.startedAtNanos = startedAtNanos;
        }

        @Override
        public void continueOrPropagate(RetryableException exception) {
            if (!isIdempotent(exception.method())) {
                throw exception;
            }
            if (retriesAttempted >= maxRetries) {
                throw exception;
            }
            if (wouldExceedBudget()) {
                throw exception;
            }
            retriesAttempted++;
            sleep();
        }

        @Override
        public Retryer clone() {
            return new BudgetAwareRetryer(maxRetries, intervalMillis, totalBudgetMillis, idempotentMethods);
        }

        private boolean isIdempotent(Request.HttpMethod method) {
            return method != null && idempotentMethods.contains(method.name());
        }

        private boolean wouldExceedBudget() {
            if (totalBudgetMillis <= 0) {
                return true;
            }
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
            return elapsedMillis + intervalMillis >= totalBudgetMillis;
        }

        private void sleep() {
            if (intervalMillis <= 0) {
                return;
            }
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RetryableException(-1, "Interrupted while waiting to retry",
                        Request.HttpMethod.GET, ex, (Long) null, null);
            }
        }

        private static Set<String> normalizeMethods(Set<String> methods) {
            if (methods == null || methods.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> normalized = new HashSet<>();
            for (String method : methods) {
                if (hasText(method)) {
                    normalized.add(method.trim().toUpperCase(Locale.ROOT));
                }
            }
            return Collections.unmodifiableSet(normalized);
        }
    }

    public static final class MeteredFeignClient implements Client {
        private final Client delegate;
        private final MeterRegistry registry;

        MeteredFeignClient(Client delegate, MeterRegistry registry) {
            this.delegate = delegate;
            this.registry = registry;
        }

        @Override
        public Response execute(Request request, Request.Options options) throws IOException {
            try {
                return delegate.execute(request, options);
            } finally {
                if (registry != null) {
                    registry.counter("framework.feign.client.requests", "method", request.httpMethod().name()).increment();
                }
            }
        }
    }

    static final class RetryableStatusErrorDecoder implements ErrorDecoder {
        private final Set<Integer> retryableStatuses;
        private final ErrorDecoder delegate = new ErrorDecoder.Default();

        RetryableStatusErrorDecoder(java.util.List<Integer> retryableStatuses) {
            this.retryableStatuses = retryableStatuses == null ? Set.of() : Set.copyOf(retryableStatuses);
        }

        @Override
        public Exception decode(String methodKey, Response response) {
            if (retryableStatuses.contains(response.status())) {
                return new RetryableException(response.status(), "Retryable HTTP status " + response.status(),
                        response.request().httpMethod(), (Long) null, response.request());
            }
            return delegate.decode(methodKey, response);
        }
    }

    private static int boundedTimeout(int configuredTimeout, int totalBudget) {
        if (totalBudget <= 0) {
            return configuredTimeout;
        }
        if (configuredTimeout <= 0) {
            return totalBudget;
        }
        return Math.min(configuredTimeout, totalBudget);
    }

    private static void addHeaderIfPresent(RequestTemplate template, String name, String value) {
        if (hasText(name) && hasText(value)) {
            template.header(name, value.trim());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
