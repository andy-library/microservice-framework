package com.microservice.framework.security.autoconfigure;

import com.microservice.framework.security.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/** Authenticates explicitly protected internal endpoints with a local service token. */
public final class ServiceIdentityAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityProperties.ServiceIdentityProperties properties;
    private final List<AntPathRequestMatcher> protectedPaths;

    public ServiceIdentityAuthenticationFilter(SecurityProperties.ServiceIdentityProperties properties) {
        this.properties = properties;
        this.protectedPaths = properties.getProtectedPaths().stream()
                .map(AntPathRequestMatcher::new)
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled() || protectedPaths.stream().noneMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String suppliedToken = request.getHeader(properties.getServiceTokenHeader());
        if (!constantTimeEquals(properties.getServiceToken(), suppliedToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String serviceId = request.getHeader(properties.getServiceIdHeader());
        if (serviceId == null || serviceId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                serviceId, null, List.of(new SimpleGrantedAuthority("ROLE_TRUSTED_SERVICE")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
