package com.microservice.framework.feign;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Feign Starter 配置属性
 * <p>
 * 聚合连接、重试、熔断、上下文传播、服务身份等配置组，
 * 所有属性前缀为 {@code framework.feign}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.feign")
public class FeignProperties {

    /**
     * 连接配置
     */
    @NestedConfigurationProperty
    private ConnectionProperties connection = new ConnectionProperties();

    /**
     * 重试配置
     */
    @NestedConfigurationProperty
    private RetryProperties retry = new RetryProperties();

    /**
     * 熔断配置
     */
    @NestedConfigurationProperty
    private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();

    /**
     * 隔离配置
     */
    @NestedConfigurationProperty
    private IsolationProperties isolation = new IsolationProperties();

    /**
     * 上下文传播配置
     */
    @NestedConfigurationProperty
    private ContextProperties context = new ContextProperties();

    /**
     * 服务身份配置
     */
    @NestedConfigurationProperty
    private ServiceIdentityProperties serviceIdentity = new ServiceIdentityProperties();

    // Getters and Setters

    public ConnectionProperties getConnection() {
        return connection;
    }

    public void setConnection(ConnectionProperties connection) {
        this.connection = connection;
    }

    public RetryProperties getRetry() {
        return retry;
    }

    public void setRetry(RetryProperties retry) {
        this.retry = retry;
    }

    public CircuitBreakerProperties getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreakerProperties circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public IsolationProperties getIsolation() {
        return isolation;
    }

    public void setIsolation(IsolationProperties isolation) {
        this.isolation = isolation;
    }

    public ContextProperties getContext() {
        return context;
    }

    public void setContext(ContextProperties context) {
        this.context = context;
    }

    public ServiceIdentityProperties getServiceIdentity() {
        return serviceIdentity;
    }

    public void setServiceIdentity(ServiceIdentityProperties serviceIdentity) {
        this.serviceIdentity = serviceIdentity;
    }

    /**
     * 连接配置 — 超时参数
     */
    public static class ConnectionProperties {

        /**
         * 全局超时时间（毫秒），默认 5000
         * <p>
         * 当未分别指定连接超时和读取超时时，以此值作为总超时上限。
         */
        private int timeout = 5000;

        /**
         * 连接超时时间（毫秒），默认 2000
         * <p>
         * 建立 TCP 连接的最大等待时间。
         */
        private int connectTimeout = 2000;

        /**
         * 读取超时时间（毫秒），默认 5000
         * <p>
         * 等待响应数据的最大时间。
         */
        private int readTimeout = 5000;

        private int maxConnections = 100;

        private int maxConnectionsPerRoute = 20;

        private long connectionTimeToLive = 30000;

        // Getters and Setters

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        public int getMaxConnectionsPerRoute() { return maxConnectionsPerRoute; }
        public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) { this.maxConnectionsPerRoute = maxConnectionsPerRoute; }
        public long getConnectionTimeToLive() { return connectionTimeToLive; }
        public void setConnectionTimeToLive(long connectionTimeToLive) { this.connectionTimeToLive = connectionTimeToLive; }
    }

    /**
     * 重试配置
     */
    public static class RetryProperties {

        /**
         * 是否启用重试，默认 true
         */
        private boolean enabled = true;

        /**
         * 最大重试次数，默认 3
         */
        private int maxRetries = 3;

        /**
         * 重试间隔（毫秒），默认 100
         */
        private int retryInterval = 100;

        /**
         * 触发重试的 HTTP 状态码，默认 [502, 503]
         */
        private List<Integer> retryOnStatuses = new ArrayList<>(Arrays.asList(502, 503));

        /**
         * 触发重试的异常类名，默认为空
         */
        private List<String> retryOnExceptions = new ArrayList<>();

        /**
         * 允许重试的幂等 HTTP 方法
         */
        private Set<String> idempotentMethods = new HashSet<>(Arrays.asList(
                "GET", "HEAD", "PUT", "DELETE", "OPTIONS", "TRACE"));

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getRetryInterval() {
            return retryInterval;
        }

        public void setRetryInterval(int retryInterval) {
            this.retryInterval = retryInterval;
        }

        public List<Integer> getRetryOnStatuses() {
            return retryOnStatuses;
        }

        public void setRetryOnStatuses(List<Integer> retryOnStatuses) {
            this.retryOnStatuses = retryOnStatuses;
        }

        public List<String> getRetryOnExceptions() {
            return retryOnExceptions;
        }

        public void setRetryOnExceptions(List<String> retryOnExceptions) {
            this.retryOnExceptions = retryOnExceptions;
        }

        public Set<String> getIdempotentMethods() {
            return idempotentMethods;
        }

        public void setIdempotentMethods(Set<String> idempotentMethods) {
            this.idempotentMethods = idempotentMethods;
        }
    }

    /**
     * 熔断配置
     */
    public static class CircuitBreakerProperties {

        /**
         * 是否启用熔断，默认 false
         * <p>
         * 需要额外引入 resilience4j 依赖才能激活。
         */
        private boolean enabled = false;

        /**
         * 失败率阈值（百分比），默认 50
         * <p>
         * 当失败率超过此阈值时，熔断器从 CLOSED 跳转到 OPEN。
         */
        private float failureRateThreshold = 50;

        /**
         * 慢调用持续时间阈值（毫秒），默认 3000
         * <p>
         * 超过此时间的调用被标记为"慢调用"。
         */
        private long slowCallDuration = 3000;

        /**
         * 慢调用率阈值（百分比），默认 100
         * <p>
         * 当慢调用率超过此阈值时，熔断器从 CLOSED 跳转到 OPEN。
         */
        private float slowCallRateThreshold = 100;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public float getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(float failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public long getSlowCallDuration() {
            return slowCallDuration;
        }

        public void setSlowCallDuration(long slowCallDuration) {
            this.slowCallDuration = slowCallDuration;
        }

        public float getSlowCallRateThreshold() {
            return slowCallRateThreshold;
        }

        public void setSlowCallRateThreshold(float slowCallRateThreshold) {
            this.slowCallRateThreshold = slowCallRateThreshold;
        }
    }

    /**
     * 上下文传播配置
     */
    public static class ContextProperties {

        /**
         * 是否启用上下文传播，默认 true
         * <p>
         * 启用后，Feign 调用将自动携带 requestId、traceId、userId 等上下文键。
         */
        private boolean propagationEnabled = true;

        /**
         * 需要传播的上下文键，默认 [requestId, traceId, userId]
         */
        private Set<String> propagateKeys = new HashSet<>(Arrays.asList(
                "requestId", "traceId", "userId"));

        // Getters and Setters

        public boolean isPropagationEnabled() {
            return propagationEnabled;
        }

        public void setPropagationEnabled(boolean propagationEnabled) {
            this.propagationEnabled = propagationEnabled;
        }

        public Set<String> getPropagateKeys() {
            return propagateKeys;
        }

        public void setPropagateKeys(Set<String> propagateKeys) {
            this.propagateKeys = propagateKeys;
        }
    }

    /**
     * 隔离配置
     */
    public static class IsolationProperties {

        /**
         * 是否启用隔离，默认 false
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 服务身份配置
     */
    public static class ServiceIdentityProperties {

        /**
         * 是否启用服务身份注入，默认 true
         * <p>
         * 启用后，Feign 调用将自动携带调用服务的身份信息。
         */
        private boolean enabled = true;

        /**
         * 当前服务名称
         * <p>
         * 用于在跨服务调用中标识调用方身份。
         */
        private String serviceName;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }
    }
}
