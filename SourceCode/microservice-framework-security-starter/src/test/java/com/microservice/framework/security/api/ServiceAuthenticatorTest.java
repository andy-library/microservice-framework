package com.microservice.framework.security.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceAuthenticator interface contract tests.
 * <p>
 * Verifies that the interface methods are correctly defined
 * and that a stub implementation fulfills the contract.
 *
 * @author Andy Yang
 */
class ServiceAuthenticatorTest {

    // ======================================================================
    // Interface contract verification
    // ======================================================================

    @Nested
    @DisplayName("接口方法契约验证")
    class InterfaceContract {

        @Test
        @DisplayName("authenticate() 应返回 SecurityContext")
        void authenticateShouldReturnSecurityContext() {
            ServiceAuthenticator authenticator = new StubAuthenticator();
            SecurityContext context = authenticator.authenticate();

            assertThat(context).isNotNull();
            assertThat(context.getUserId()).isEqualTo("stub-user");
        }

        @Test
        @DisplayName("getCurrentUser() 应返回 SecurityContext")
        void getCurrentUserShouldReturnSecurityContext() {
            ServiceAuthenticator authenticator = new StubAuthenticator();
            SecurityContext context = authenticator.getCurrentUser();

            assertThat(context).isNotNull();
            assertThat(context.getUserId()).isEqualTo("stub-user");
        }

        @Test
        @DisplayName("getCurrentServiceId() 应返回服务标识")
        void getCurrentServiceIdShouldReturnServiceId() {
            ServiceAuthenticator authenticator = new StubAuthenticator();
            String serviceId = authenticator.getCurrentServiceId();

            assertThat(serviceId).isEqualTo("stub-service");
        }

        @Test
        @DisplayName("isServiceCall() 应返回布尔值")
        void isServiceCallShouldReturnBoolean() {
            ServiceAuthenticator authenticator = new StubAuthenticator();
            boolean result = authenticator.isServiceCall();

            assertThat(result).isTrue();
        }
    }

    // ======================================================================
    // Stub implementation for contract testing
    // ======================================================================

    private static class StubAuthenticator implements ServiceAuthenticator {

        @Override
        public SecurityContext authenticate() {
            return SecurityContext.of("stub-user", java.util.Set.of("admin"),
                    java.util.Set.of("order:create"), "stub-service", true);
        }

        @Override
        public SecurityContext getCurrentUser() {
            return authenticate();
        }

        @Override
        public String getCurrentServiceId() {
            return "stub-service";
        }

        @Override
        public boolean isServiceCall() {
            return true;
        }
    }
}
