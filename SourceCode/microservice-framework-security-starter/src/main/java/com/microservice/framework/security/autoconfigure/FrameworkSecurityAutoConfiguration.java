package com.microservice.framework.security.autoconfigure;

import com.microservice.framework.security.SecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Framework Security 基础自动配置
 * <p>
 * 根据 {@code framework.security.cors.enabled} 和
 * {@code framework.security.path} 属性配置 CORS 和路径安全规则。
 * <p>
 * 仅在 {@code SecurityFilterChain} 类存在于类路径时激活，
 * 即要求 {@code spring-boot-starter-security} 在依赖中。
 * <p>
 * 本配置在 Spring Boot 默认安全配置之前运行，以便覆盖默认的
 * SecurityFilterChain 并注入自定义的 CORS 和路径规则。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(prefix = "framework.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
public class FrameworkSecurityAutoConfiguration {

    /**
     * Creates the default security filter chain with CORS and path rules.
     * <p>
     * Configures:
     * <ul>
     *   <li>CORS policy based on {@code framework.security.cors} properties</li>
     *   <li>Permit paths from {@code framework.security.path.permit-paths}</li>
     *   <li>Deny paths from {@code framework.security.path.deny-paths}</li>
     *   <li>All other paths require authentication</li>
     * </ul>
     *
     * @param http       the HttpSecurity builder
     * @param properties the security configuration properties
     * @return a configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties properties) throws Exception {
        // Configure CORS if enabled
        if (properties.getCors().isEnabled()) {
            http.cors(cors -> cors.configurationSource(request -> {
                var config = new org.springframework.web.cors.CorsConfiguration();
                config.setAllowedOrigins(properties.getCors().getAllowedOrigins());
                config.setAllowedMethods(properties.getCors().getAllowedMethods());
                config.setAllowedHeaders(java.util.List.of("*"));
                config.setAllowCredentials(true);
                config.setMaxAge(properties.getCors().getMaxAge());
                return config;
            }));
        }

        // Configure path-based authorization
        http.authorizeHttpRequests(authz -> {
            // Deny paths - explicitly forbidden
            for (String denyPath : properties.getPath().getDenyPaths()) {
                authz.requestMatchers(denyPath).denyAll();
            }
            // Permit paths - no authentication required
            for (String permitPath : properties.getPath().getPermitPaths()) {
                authz.requestMatchers(permitPath).permitAll();
            }
            // All other paths require authentication
            authz.anyRequest().authenticated();
        });

        // Disable CSRF for REST APIs (typical for microservices)
        http.csrf(csrf -> csrf.disable());

        return http.build();
    }
}
