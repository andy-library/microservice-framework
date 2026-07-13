package com.microservice.framework.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Security Starter 配置属性
 * <p>
 * 聚合 JWT、服务身份、权限、CORS 和路径安全等配置组，
 * 所有属性前缀为 {@code framework.security}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.security")
public class SecurityProperties {

    /**
     * JWT 认证配置
     */
    @NestedConfigurationProperty
    private JwtProperties jwt = new JwtProperties();

    /**
     * 服务身份配置
     */
    @NestedConfigurationProperty
    private ServiceIdentityProperties serviceIdentity = new ServiceIdentityProperties();

    /**
     * 权限配置
     */
    @NestedConfigurationProperty
    private PermissionProperties permission = new PermissionProperties();

    /**
     * CORS 跨域配置
     */
    @NestedConfigurationProperty
    private CorsProperties cors = new CorsProperties();

    /**
     * 路径安全规则配置
     */
    @NestedConfigurationProperty
    private PathProperties path = new PathProperties();

    // Getters and Setters

    public JwtProperties getJwt() {
        return jwt;
    }

    public void setJwt(JwtProperties jwt) {
        this.jwt = jwt;
    }

    public ServiceIdentityProperties getServiceIdentity() {
        return serviceIdentity;
    }

    public void setServiceIdentity(ServiceIdentityProperties serviceIdentity) {
        this.serviceIdentity = serviceIdentity;
    }

    public PermissionProperties getPermission() {
        return permission;
    }

    public void setPermission(PermissionProperties permission) {
        this.permission = permission;
    }

    public CorsProperties getCors() {
        return cors;
    }

    public void setCors(CorsProperties cors) {
        this.cors = cors;
    }

    public PathProperties getPath() {
        return path;
    }

    public void setPath(PathProperties path) {
        this.path = path;
    }

    // ======================================================================
    // Nested configuration classes
    // ======================================================================

    /**
     * JWT 认证配置
     * <p>
     * 控制 Keycloak JWT 验证行为，包括 issuer、JWK set、
     * audience 和角色 claim key 的映射。
     */
    public static class JwtProperties {

        /**
         * 是否启用 JWT 认证，默认 true
         */
        private boolean enabled = true;

        /**
         * JWT issuer URI，用于验证 token 的签发者
         * <p>
         * 通常为 Keycloak 的 realm URL，例如:
         * {@code https://keycloak.example.com/realms/my-realm}
         */
        private String issuerUri;

        /**
         * JWK Set URI，用于获取公钥验证 JWT 签名
         * <p>
         * 通常为 Keycloak 的 JWKS endpoint，例如:
         * {@code https://keycloak.example.com/realms/my-realm/protocol/openid-connect/certs}
         */
        private String jwkSetUri;

        /**
         * JWT audience 验证值
         * <p>
         * 用于验证 token 的 audience claim 是否匹配本服务。
         */
        private String audience;

        /**
         * JWT 中角色信息的 claim key，默认 "roles"
         * <p>
         * Keycloak 默认将角色放在 "realm_access.roles" 中，
         * 此属性允许自定义角色 claim 的提取路径。
         */
        private String claimRolesKey = "roles";

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getClaimRolesKey() {
            return claimRolesKey;
        }

        public void setClaimRolesKey(String claimRolesKey) {
            this.claimRolesKey = claimRolesKey;
        }
    }

    /**
     * 服务身份配置
     * <p>
     * 控制服务间调用的身份验证，通过 service token header
     * 标识调用来源服务。
     */
    public static class ServiceIdentityProperties {

        /**
         * 是否启用服务身份验证，默认 false
         */
        private boolean enabled = false;

        /**
         * 服务 Token 请求头名称，默认 "X-Service-Token"
         * <p>
         * 服务间调用时，调用方在 HTTP 请求头中携带此字段，
         * 接收方通过验证此 token 确认调用来源。
         */
        private String serviceTokenHeader = "X-Service-Token";

        /**
         * 静态服务凭据。启用服务身份时必须显式配置或由自定义认证组件提供。
         */
        private String serviceToken;

        /** Calling service identity header. */
        private String serviceIdHeader = "X-Service-Id";

        /** Paths that require trusted service authentication. */
        private List<String> protectedPaths = new ArrayList<>();

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServiceTokenHeader() {
            return serviceTokenHeader;
        }

        public void setServiceTokenHeader(String serviceTokenHeader) {
            this.serviceTokenHeader = serviceTokenHeader;
        }

        public String getServiceToken() {
            return serviceToken;
        }

        public void setServiceToken(String serviceToken) {
            this.serviceToken = serviceToken;
        }

        public String getServiceIdHeader() {
            return serviceIdHeader;
        }

        public void setServiceIdHeader(String serviceIdHeader) {
            this.serviceIdHeader = serviceIdHeader;
        }

        public List<String> getProtectedPaths() {
            return protectedPaths;
        }

        public void setProtectedPaths(List<String> protectedPaths) {
            this.protectedPaths = protectedPaths == null ? new ArrayList<>() : new ArrayList<>(protectedPaths);
        }
    }

    /**
     * 权限配置
     * <p>
     * 控制接口级别的权限检查，从 JWT claims 中提取
     * 权限列表用于细粒度鉴权。
     */
    public static class PermissionProperties {

        /**
         * 是否启用权限检查，默认 true
         */
        private boolean enabled = true;

        /**
         * JWT 中权限信息的 claim key，默认 "permissions"
         * <p>
         * 指定从 JWT claims 中提取权限列表的 key，
         * 权限格式通常为 {@code resource:action}。
         */
        private String permissionClaimKey = "permissions";

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPermissionClaimKey() {
            return permissionClaimKey;
        }

        public void setPermissionClaimKey(String permissionClaimKey) {
            this.permissionClaimKey = permissionClaimKey;
        }
    }

    /**
     * CORS 跨域配置
     * <p>
     * 控制跨域资源共享策略，适用于前后端分离架构
     * 或微服务间的跨域访问场景。
     */
    public static class CorsProperties {

        /**
         * 是否启用 CORS 配置，默认 true
         */
        private boolean enabled = true;

        /**
         * 允许的源列表
         * <p>
         * 未配置时默认允许所有源 ({@code *})。
         * 生产环境应配置为具体域名列表。
         */
        private List<String> allowedOrigins = new ArrayList<>(Arrays.asList("*"));

        /**
         * 允许的 HTTP 方法列表
         * <p>
         * 未配置时默认允许常用方法: GET, POST, PUT, DELETE, PATCH, OPTIONS。
         */
        private List<String> allowedMethods = new ArrayList<>(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        /**
         * 预检请求缓存时间（秒），默认 3600
         */
        private long maxAge = 3600;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    /**
     * 路径安全规则配置
     * <p>
     * 定义哪些路径无需认证即可访问（permit），
     * 哪些路径即使认证也不可访问（deny）。
     */
    public static class PathProperties {

        /**
         * 允许未经认证访问的路径模式列表
         * <p>
         * 默认仅允许 Kubernetes 健康与就绪探针。
         */
        private List<String> permitPaths = new ArrayList<>(Arrays.asList(
                "/actuator/health",
                "/actuator/health/liveness",
                "/actuator/health/readiness"));

        /**
         * 禁止访问的路径模式列表
         * <p>
         * 即使认证通过也不允许访问的路径，默认为空。
         */
        private List<String> denyPaths = new ArrayList<>();

        // Getters and Setters

        public List<String> getPermitPaths() {
            return permitPaths;
        }

        public void setPermitPaths(List<String> permitPaths) {
            this.permitPaths = permitPaths;
        }

        public List<String> getDenyPaths() {
            return denyPaths;
        }

        public void setDenyPaths(List<String> denyPaths) {
            this.denyPaths = denyPaths;
        }
    }
}
