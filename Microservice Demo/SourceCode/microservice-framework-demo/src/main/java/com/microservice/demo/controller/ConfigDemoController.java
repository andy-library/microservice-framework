package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * config center demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/config")
public class ConfigDemoController {

    private final ApplicationContext context;
    private final Environment environment;
    private final ObjectProvider<com.microservice.framework.nacos.config.SensitiveConfigMasker> nacosMasker;
    private final ObjectProvider<com.microservice.framework.apollo.config.SensitiveConfigMasker> apolloMasker;

    public ConfigDemoController(ApplicationContext context,
                                Environment environment,
                                ObjectProvider<com.microservice.framework.nacos.config.SensitiveConfigMasker> nacosMasker,
                                ObjectProvider<com.microservice.framework.apollo.config.SensitiveConfigMasker> apolloMasker) {
        this.context = context;
        this.environment = environment;
        this.nacosMasker = nacosMasker;
        this.apolloMasker = apolloMasker;
    }

    @GetMapping("/source")
    public ApiResponse<Map<String, Object>> source() {
        return ApiResponse.success(Map.of(
                "source", "framework-config",
                "enabled", environment.getProperty("framework.config.enabled", Boolean.class, false),
                "nacosAvailable", nacosMasker.getIfAvailable() != null,
                "apolloAvailable", apolloMasker.getIfAvailable() != null,
                "activeMasker", activeMaskerName(),
                "sensitiveKeyPatterns", activePatterns()));
    }

    @GetMapping({"/governance", "/masked"})
    public ApiResponse<Map<String, Object>> governance() {
        return ApiResponse.success(Map.of(
                "activeMasker", activeMaskerName(),
                "maskValue", activeMaskValue(),
                "comparison", Map.of(
                        "db.password", mask("db.password", "secret"),
                        "redis.host", mask("redis.host", "127.0.0.1"))));
    }

    @GetMapping("/validation")
    public ApiResponse<Map<String, Object>> validation() {
        return ApiResponse.success(Map.of(
                "activeMasker", activeMaskerName(),
                "sensitivityCheck", Map.of(
                        "database.password", isSensitive("database.password"),
                        "kafka.bootstrap.servers", isSensitive("kafka.bootstrap.servers")),
                "envConfigCheck", Map.of(
                        "spring.application.name", environment.getProperty("spring.application.name", ""))));
    }

    private String activeMaskerName() {
        if (nacosMasker.getIfAvailable() != null) {
            return context.containsBean("nacosSensitiveConfigMasker") ? "nacosSensitiveConfigMasker" : "nacos";
        }
        if (apolloMasker.getIfAvailable() != null) {
            return context.containsBean("apolloSensitiveConfigMasker") ? "apolloSensitiveConfigMasker" : "apollo";
        }
        return "none";
    }

    private Object activePatterns() {
        if (nacosMasker.getIfAvailable() != null) {
            return nacosMasker.getObject().getSensitiveKeyPatterns();
        }
        if (apolloMasker.getIfAvailable() != null) {
            return apolloMasker.getObject().getSensitiveKeyPatterns();
        }
        return java.util.Set.of();
    }

    private String activeMaskValue() {
        if (nacosMasker.getIfAvailable() != null) {
            return nacosMasker.getObject().getMaskValue();
        }
        if (apolloMasker.getIfAvailable() != null) {
            return apolloMasker.getObject().getMaskValue();
        }
        return "";
    }

    private boolean isSensitive(String key) {
        if (nacosMasker.getIfAvailable() != null) {
            return nacosMasker.getObject().isSensitive(key);
        }
        return apolloMasker.getIfAvailable() != null && apolloMasker.getObject().isSensitive(key);
    }

    private String mask(String key, String value) {
        if (nacosMasker.getIfAvailable() != null) {
            return nacosMasker.getObject().mask(key, value);
        }
        if (apolloMasker.getIfAvailable() != null) {
            return apolloMasker.getObject().mask(key, value);
        }
        return value;
    }
}
