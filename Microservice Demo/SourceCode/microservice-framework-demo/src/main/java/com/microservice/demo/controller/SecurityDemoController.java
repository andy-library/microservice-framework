package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.stream.Collectors;

import java.util.Map;

/**
 * security-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/security")
public class SecurityDemoController {

    @GetMapping("/public")
    public ApiResponse<Map<String, Object>> publicEndpoint() {
        return ApiResponse.success(Map.of("access", "public"));
    }

    @GetMapping("/user")
    public ApiResponse<Map<String, Object>> userEndpoint(Authentication authentication) {
        if (authentication == null) {
            return ApiResponse.success(Map.of("access", "user", "username", "anonymous",
                    "authorities", java.util.List.of()));
        }
        return ApiResponse.success(Map.of(
                "access", "user",
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().stream()
                        .map(Object::toString)
                        .collect(Collectors.toList())));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> adminEndpoint() {
        return ApiResponse.success(Map.of("access", "admin"));
    }

    @GetMapping("/internal")
    public ApiResponse<Map<String, Object>> internalEndpoint(Authentication authentication) {
        return ApiResponse.success(Map.of("access", "internal",
                "serviceId", authentication == null ? "anonymous" : authentication.getName()));
    }
}
