package com.microservice.demo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Opt-in real Apollo profile acceptance test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"full-embedded", "config-apollo"})
@EnabledIfSystemProperty(named = "demo.real.middleware.acceptance", matches = "true")
@DisplayName("真实 Apollo 配置中心 profile 验收")
class RealApolloConfigAcceptanceTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("Apollo 单独启用，Nacos 关闭，配置治理 Bean 存在")
    void apolloProfileStartsWithGovernanceOnly() throws Exception {
        assertEquals("true", environment.getProperty("framework.apollo.enabled"));
        assertEquals("false", environment.getProperty("framework.nacos.enabled"));
        assertEquals("true", environment.getProperty("framework.config.enabled"));

        assertTrue(context.containsBean("apolloConfigValidator"));
        assertTrue(context.containsBean("apolloSensitiveConfigMasker"));
        assertFalse(context.containsBean("nacosConfigValidator"));
        assertFalse(context.containsBean("nacosSensitiveConfigMasker"));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        String portalUrl = System.getProperty("demo.apollo.portal.url",
                System.getenv().getOrDefault("DEMO_APOLLO_PORTAL_URL", "http://localhost:8070"));
        String configServiceUrl = System.getProperty("demo.apollo.config-service.url",
                System.getenv().getOrDefault("DEMO_APOLLO_CONFIG_SERVICE_URL", "http://localhost:8080"));
        String adminServiceUrl = System.getProperty("demo.apollo.admin-service.url",
                System.getenv().getOrDefault("DEMO_APOLLO_ADMIN_SERVICE_URL", "http://localhost:8090"));

        HttpResponse<Void> portal = client.send(HttpRequest.newBuilder()
                        .uri(URI.create(portalUrl))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(302, portal.statusCode());

        HttpResponse<Void> configService = client.send(HttpRequest.newBuilder()
                        .uri(URI.create(configServiceUrl))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        assertReachableStatus(configService.statusCode());

        HttpResponse<Void> adminService = client.send(HttpRequest.newBuilder()
                        .uri(URI.create(adminServiceUrl))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        assertReachableStatus(adminService.statusCode());
    }

    private void assertReachableStatus(int statusCode) {
        assertTrue((statusCode >= 200 && statusCode < 400) || statusCode == 403,
                "Apollo service should be reachable, but status was " + statusCode);
    }

    @Test
    @DisplayName("Apollo profile 下配置治理 Controller API 可用")
    void apolloConfigGovernanceApisWork() {
        given().when().get("/demo/config/source")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.apolloAvailable", notNullValue())
                .body("data.nacosAvailable", equalTo(false))
                .body("data.activeMasker", equalTo("apolloSensitiveConfigMasker"))
                .body("data.sensitiveKeyPatterns", notNullValue());

        given().when().get("/demo/config/masked")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.maskValue", equalTo("***"))
                .body("data.activeMasker", equalTo("apolloSensitiveConfigMasker"))
                .body("data.comparison", hasKey("db.password"))
                .body("data.comparison", hasKey("redis.host"));

        given().when().get("/demo/config/validation")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.activeMasker", equalTo("apolloSensitiveConfigMasker"))
                .body("data.sensitivityCheck.'database.password'", equalTo(true))
                .body("data.sensitivityCheck.'kafka.bootstrap.servers'", equalTo(false))
                .body("data.envConfigCheck.'spring.application.name'", notNullValue());
    }
}
