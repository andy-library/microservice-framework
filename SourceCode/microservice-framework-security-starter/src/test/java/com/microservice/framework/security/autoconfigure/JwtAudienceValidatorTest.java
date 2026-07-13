package com.microservice.framework.security.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAudienceValidatorTest {

    @Test
    @DisplayName("AudienceValidator: token audience 不匹配时拒绝")
    void rejectsMissingAudience() {
        JwtAutoConfiguration.AudienceValidator validator =
                new JwtAutoConfiguration.AudienceValidator("orders");
        Jwt jwt = jwtWithAudience(List.of("payments"));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("AudienceValidator: token audience 匹配时通过")
    void acceptsMatchingAudience() {
        JwtAutoConfiguration.AudienceValidator validator =
                new JwtAutoConfiguration.AudienceValidator("orders");
        Jwt jwt = jwtWithAudience(List.of("orders", "payments"));

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    private static Jwt jwtWithAudience(List<String> audience) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "user-1", "aud", audience));
    }
}
