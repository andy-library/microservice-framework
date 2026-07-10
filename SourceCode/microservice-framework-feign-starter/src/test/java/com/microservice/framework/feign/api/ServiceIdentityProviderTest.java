package com.microservice.framework.feign.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceIdentityProvider 接口契约测试
 * <p>
 * 验证接口方法的基本契约：getServiceId 返回非空值，
 * getServiceToken 返回非空值，getHeaders 返回非空 Map。
 *
 * @author Andy Yang
 */
class ServiceIdentityProviderTest {

    @Nested
    @DisplayName("接口方法契约")
    class InterfaceContract {

        @Test
        @DisplayName("getServiceId 应返回非空字符串")
        void getServiceIdShouldReturnNonNull() {
            ServiceIdentityProvider provider = new StubProvider("order-service", "token-123");
            assertThat(provider.getServiceId()).isEqualTo("order-service");
        }

        @Test
        @DisplayName("getServiceToken 应返回非空字符串")
        void getServiceTokenShouldReturnNonNull() {
            ServiceIdentityProvider provider = new StubProvider("order-service", "token-123");
            assertThat(provider.getServiceToken()).isEqualTo("token-123");
        }

        @Test
        @DisplayName("getHeaders 应返回非空 Map")
        void getHeadersShouldReturnNonNullMap() {
            ServiceIdentityProvider provider = new StubProvider("order-service", "token-123");
            Map<String, String> headers = provider.getHeaders();
            assertThat(headers).isNotNull();
            assertThat(headers).isNotEmpty();
        }

        @Test
        @DisplayName("getHeaders 可包含自定义头")
        void getHeadersCanContainCustomHeaders() {
            ServiceIdentityProvider provider = new StubProvider("order-service", "token-123");
            assertThat(provider.getHeaders()).containsEntry("X-Service-Id", "order-service");
        }
    }

    // Stub implementation for contract testing
    static class StubProvider implements ServiceIdentityProvider {

        private final String serviceId;
        private final String serviceToken;

        StubProvider(String serviceId, String serviceToken) {
            this.serviceId = serviceId;
            this.serviceToken = serviceToken;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public String getServiceToken() {
            return serviceToken;
        }

        @Override
        public Map<String, String> getHeaders() {
            return Map.of("X-Service-Id", serviceId, "X-Service-Token", serviceToken);
        }
    }
}
