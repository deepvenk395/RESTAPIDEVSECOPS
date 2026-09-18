package com.devsecops.taskflow.security;

import com.devsecops.taskflow.config.AppProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private static final String STRONG_SECRET = "!".repeat(48);

    private JwtService newJwtService(String secret, long expirationSeconds) {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt(secret, expirationSeconds),
                new AppProperties.Cors("http://localhost:3000"),
                new AppProperties.Seed(false, false, new AppProperties.Admin("admin", "password12345"))
        );
        return new JwtService(environment, properties);
    }

    private UserDetails user(String username) {
        return User.withUsername(username)
                .password("hunter2")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void issuedTokenContainsSubjectAndRoles() {
        JwtService service = newJwtService(STRONG_SECRET, 3600);

        var token = service.issue(user("alice"));

        assertThat(service.extractUsername(token.value())).isEqualTo("alice");
        assertThat(token.expiresAt()).isAfter(Instant.now());
        assertThat(token.value()).isNotBlank();

        var roles = service.extractClaim(token.value(), claims -> claims.get("roles", java.util.List.class));
        assertThat(roles).contains("ROLE_USER");
    }

    @Test
    void expirationIsApplied() {
        JwtService service = newJwtService(STRONG_SECRET, 3600);
        var token = service.issue(user("bob"));
        Instant expiresAt = service.extractClaim(token.value(), claims -> claims.getExpiration().toInstant());
        assertThat(expiresAt).isCloseTo(token.expiresAt(), within(2, ChronoUnit.SECONDS));
    }

    @Test
    void validTokenPassesForSameUserAndFailsForAnother() {
        JwtService service = newJwtService(STRONG_SECRET, 3600);
        UserDetails alice = user("alice");
        String token = service.issue(alice).value();

        assertThat(service.isTokenValid(token, alice)).isTrue();
        assertThat(service.isTokenValid(token, user("mallory"))).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = newJwtService(STRONG_SECRET, 3600);
        String token = service.issue(user("alice")).value();

        assertThatThrownBy(() -> service.extractUsername(token + "tampered"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void weakSecretIsRejected() {
        assertThatThrownBy(() -> newJwtService("way-too-short", 3600))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }

    @Test
    void nonPositiveExpirationIsRejected() {
        assertThatThrownBy(() -> newJwtService(STRONG_SECRET, 0))
                .isInstanceOf(IllegalStateException.class);
    }
}