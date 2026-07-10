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
 * Opt-in real Nacos profile acceptance test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"full-embedded", "config-nacos"})
@EnabledIfSystemProperty(named = "demo.real.middleware.acceptance", matches = "true")
@DisplayName("真实 Nacos 配置中心 profile 验收")
class RealNacosConfigAcceptanceTest {

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
    @DisplayName("Nacos 单独启用，Apollo 关闭，配置治理 Bean 存在")
    void nacosProfileStartsWithGovernanceOnly() throws Exception {
        assertEquals("true", environment.getProperty("framework.nacos.enabled"));
        assertEquals("false", environment.getProperty("framework.apollo.enabled"));
        assertEquals("true", environment.getProperty("framework.config.enabled"));

        assertTrue(context.containsBean("nacosConfigValidator"));
        assertTrue(context.containsBean("nacosSensitiveConfigMasker"));
        assertFalse(context.containsBean("apolloConfigValidator"));
        assertFalse(context.containsBean("apolloSensitiveConfigMasker"));

        String nacosUrl = System.getProperty("demo.nacos.url",
                System.getenv().getOrDefault("DEMO_NACOS_URL", "http://localhost:8848/nacos/"));

        HttpResponse<Void> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build()
                .send(HttpRequest.newBuilder()
                                .uri(URI.create(nacosUrl))
                                .timeout(Duration.ofSeconds(3))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.discarding());
        assertEquals(200, response.statusCode());
    }

    @Test
    @DisplayName("Nacos profile 下配置治理 Controller API 可用")
    void nacosConfigGovernanceApisWork() {
        given().when().get("/demo/config/source")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.nacosAvailable", notNullValue())
                .body("data.apolloAvailable", equalTo(false))
                .body("data.activeMasker", equalTo("nacosSensitiveConfigMasker"))
                .body("data.sensitiveKeyPatterns", notNullValue());

        given().when().get("/demo/config/masked")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.maskValue", equalTo("***"))
                .body("data.activeMasker", equalTo("nacosSensitiveConfigMasker"))
                .body("data.comparison", hasKey("db.password"))
                .body("data.comparison", hasKey("redis.host"));

        given().when().get("/demo/config/validation")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.activeMasker", equalTo("nacosSensitiveConfigMasker"))
                .body("data.sensitivityCheck.'database.password'", equalTo(true))
                .body("data.sensitivityCheck.'kafka.bootstrap.servers'", equalTo(false))
                .body("data.envConfigCheck.'spring.application.name'", notNullValue());
    }
}
