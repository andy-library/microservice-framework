package com.microservice.framework.security.autoconfigure;

import com.microservice.framework.security.SecurityProperties;
import com.microservice.framework.security.api.PermissionEvaluator;
import com.microservice.framework.security.api.ServiceAuthenticator;
import com.microservice.framework.security.api.SecurityContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * JWT 自动配置
 * <p>
 * 根据 {@code framework.security.jwt.enabled} 属性决定是否激活，
 * 默认启用。提供 {@link ServiceAuthenticator} 和
 * {@link PermissionEvaluator} Bean。
 * <p>
 * 仅在 {@code JwtAuthenticationToken} 类存在于类路径时激活，
 * 即要求 {@code spring-boot-starter-oauth2-resource-server} 在依赖中。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnClass(JwtAuthenticationToken.class)
@ConditionalOnProperty(prefix = "framework.security", name = {"enabled", "jwt.enabled"}, havingValue = "true", matchIfMissing = true)
public class JwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "framework.security.jwt",
            name = {"issuer-uri", "jwk-set-uri", "audience"})
    public JwtDecoder jwtDecoder(SecurityProperties properties) {
        SecurityProperties.JwtProperties jwt = properties.getJwt();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwt.getJwkSetUri()).build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(jwt.getIssuerUri()),
                new AudienceValidator(jwt.getAudience()));
        decoder.setJwtValidator(validator);
        return decoder;
    }

    /**
     * Creates a default {@link ServiceAuthenticator} bean that extracts
     * security context from JWT authentication tokens.
     *
     * @param properties the security configuration properties
     * @return a JwtServiceAuthenticator instance
     */
    @Bean
    @ConditionalOnMissingBean
    public ServiceAuthenticator serviceAuthenticator(SecurityProperties properties) {
        return new JwtServiceAuthenticator(properties);
    }

    /**
     * Creates a default {@link PermissionEvaluator} bean that checks
     * permissions and roles from the current security context.
     *
     * @param authenticator the service authenticator for context retrieval
     * @return a JwtPermissionEvaluator instance
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionEvaluator permissionEvaluator(ServiceAuthenticator authenticator) {
        return new JwtPermissionEvaluator(authenticator);
    }

    // ======================================================================
    // Inner implementation classes
    // ======================================================================

    /**
     * JWT-based implementation of {@link ServiceAuthenticator}.
     * <p>
     * Extracts security context from Spring Security's JWT authentication token,
     * converting JWT claims into the framework's SecurityContext model.
     */
    static class JwtServiceAuthenticator implements ServiceAuthenticator {

        private final SecurityProperties properties;

        JwtServiceAuthenticator(SecurityProperties properties) {
            this.properties = properties;
        }

        @Override
        public SecurityContext authenticate() {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new com.microservice.framework.common.error.FrameworkException(
                        com.microservice.framework.common.error.FrameworkErrorCode.of("SECURITY", "AUTH", 1),
                        "No authenticated user found in security context");
            }

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Map<String, Object> claims = jwtAuth.getToken().getClaims();
                return SecurityContext.fromClaims(
                        claims,
                        properties.getJwt().getClaimRolesKey(),
                        properties.getPermission().getPermissionClaimKey()
                );
            }

            // For non-JWT authentication (e.g., service token), extract from details
            String userId = authentication.getName();
            return SecurityContext.of(userId, Set.of(), Set.of(), null, false);
        }

        @Override
        public SecurityContext getCurrentUser() {
            return authenticate();
        }

        @Override
        public String getCurrentServiceId() {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Object serviceId = jwtAuth.getToken().getClaims().get("service_id");
                return serviceId != null ? serviceId.toString() : null;
            }
            return null;
        }

        @Override
        public boolean isServiceCall() {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Object serviceCall = jwtAuth.getToken().getClaims().get("service_call");
                if (serviceCall instanceof Boolean) {
                    return (Boolean) serviceCall;
                }
                if (serviceCall != null) {
                    return Boolean.parseBoolean(serviceCall.toString());
                }
                return jwtAuth.getToken().getClaims().containsKey("service_id");
            }
            return false;
        }
    }

    /**
     * JWT-based implementation of {@link PermissionEvaluator}.
     * <p>
     * Delegates to {@link ServiceAuthenticator} for context retrieval,
     * then checks permissions and roles against the SecurityContext.
     */
    static class JwtPermissionEvaluator implements PermissionEvaluator {

        private final ServiceAuthenticator authenticator;

        JwtPermissionEvaluator(ServiceAuthenticator authenticator) {
            this.authenticator = authenticator;
        }

        @Override
        public boolean hasPermission(String permission) {
            SecurityContext context = authenticator.getCurrentUser();
            return context.hasPermission(permission);
        }

        @Override
        public boolean hasRole(String role) {
            SecurityContext context = authenticator.getCurrentUser();
            return context.hasRole(role);
        }

        @Override
        public boolean hasAnyRole(Collection<String> roles) {
            SecurityContext context = authenticator.getCurrentUser();
            for (String role : roles) {
                if (context.hasRole(role)) {
                    return true;
                }
            }
            return false;
        }
    }

    static class AudienceValidator implements OAuth2TokenValidator<Jwt> {

        private final String audience;

        AudienceValidator(String audience) {
            this.audience = audience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            if (audience != null && token.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "The required audience is missing",
                    null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
