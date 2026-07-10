package com.microservice.framework.observability.autoconfigure.tracing;

import com.microservice.framework.observability.ObservabilityProperties;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Baggage 自动配置
 * 配置需要自动传播的 Baggage 字段
 * 
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnClass(OtelBaggageManager.class)
@ConditionalOnProperty(prefix = "framework.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ObservabilityProperties.class)
public class BaggageAutoConfiguration {

    /**
     * 配置 Baggage 传播字段
     * 注意：Spring Boot 3.x 通过 management.tracing.baggage.remote-fields 配置
     */
    @Configuration(proxyBeanMethods = false)
    static class BaggageConfiguration {

        @Bean
        public BaggageFieldsConfigurer baggageFieldsConfigurer(ObservabilityProperties properties) {
            return new BaggageFieldsConfigurer(properties.getTracing().getBaggageKeys());
        }
    }

    /**
     * Baggage 字段配置器
     */
    static class BaggageFieldsConfigurer {
        private final List<String> baggageKeys;

        BaggageFieldsConfigurer(List<String> baggageKeys) {
            this.baggageKeys = baggageKeys;
        }

        public List<String> getBaggageKeys() {
            return baggageKeys;
        }
    }
}
