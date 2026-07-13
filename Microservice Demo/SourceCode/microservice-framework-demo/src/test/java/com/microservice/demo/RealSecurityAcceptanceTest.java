package com.microservice.demo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * Security starter API acceptance test.
 *
 * <p>This suite verifies that framework security rules are effective through
 * real demo HTTP APIs. It intentionally uses HTTP Basic users as the local
 * acceptance substitute for the later Keycloak/JWT integration.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"full-embedded", "security-acceptance"})
@EnabledIfSystemProperty(named = "demo.real.middleware.acceptance", matches = "true")
@Import(RealSecurityAcceptanceTest.SecurityAcceptanceUsers.class)
@DisplayName("Security Starter API 验收")
class RealSecurityAcceptanceTest {

    private static final String SERVICE_SECRET = "security-acceptance-secret";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("公开接口无需认证")
    void publicEndpointAllowsAnonymousAccess() {
        given().when().get("/demo/security/public")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("public"));
    }

    @Test
    @DisplayName("默认策略保护未显式放行接口")
    void defaultPolicyRequiresAuthentication() {
        given().when().get("/demo/web/success")
                .then().statusCode(401);

        given().when().get("/demo/security/user")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("已认证用户可访问用户接口")
    void authenticatedUserCanAccessUserEndpoint() {
        given().auth().basic("demo-user", "demo-pass")
                .when().get("/demo/security/user")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("user"))
                .body("data.username", equalTo("demo-user"))
                .body("data.authorities", hasItem("ROLE_USER"));
    }

    @Test
    @DisplayName("接口级角色权限生效")
    void roleBasedMethodSecurityWorks() {
        given().auth().basic("demo-user", "demo-pass")
                .when().get("/demo/security/admin")
                .then().statusCode(403);

        given().auth().basic("demo-admin", "admin-pass")
                .when().get("/demo/security/admin")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("admin"));
    }

    @Test
    @DisplayName("内部服务接口无服务签名时拒绝访问")
    void internalEndpointRejectsUnsignedServiceCall() {
        given().when().get("/demo/security/internal")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("内部服务接口签名错误时拒绝访问")
    void internalEndpointRejectsInvalidServiceSignature() {
        given()
                .header("X-Service-Id", "order-service")
                .header("X-Service-Token", "bad-token")
                .when().get("/demo/security/internal")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("内部服务接口签名正确时允许访问")
    void internalEndpointAllowsSignedServiceCall() {
        String serviceId = "order-service";

        given()
                .header("X-Service-Id", serviceId)
                .header("X-Service-Token", SERVICE_SECRET)
                .when().get("/demo/security/internal")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("internal"))
                .body("data.serviceId", equalTo(serviceId));
    }

    @TestConfiguration
    static class SecurityAcceptanceUsers {

        @Bean
        UserDetailsService securityAcceptanceUserDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("demo-user")
                            .password("{noop}demo-pass")
                            .roles("USER")
                            .build(),
                    User.withUsername("demo-admin")
                            .password("{noop}admin-pass")
                            .roles("ADMIN")
                            .build()
            );
        }
    }
}
