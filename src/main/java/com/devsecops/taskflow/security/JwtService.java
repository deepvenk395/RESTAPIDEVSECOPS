package com.devsecops.taskflow.security;

import com.devsecops.taskflow.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JwtService {

    public static final String DEFAULT_DEV_SECRET = "dev-only-insecure-secret-change-me-in-production-!!";

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(Environment environment, AppProperties appProperties) {
        String secret = appProperties.jwt().secret();
        checkStrength(secret);

        this.signingKey = resolveSigningKey(secret);
        this.expirationSeconds = appProperties.jwt().expirationSeconds();
        if (this.expirationSeconds <= 0) {
            throw new IllegalStateException("app.jwt.expiration-seconds must be a positive number");
        }
    }

    public Token issue(UserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String value = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new Token(value, expiresAt);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername());
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    private boolean isProd(Environment environment) {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private void checkStrength(String secret) {
        int effectiveBytes;
        try {
            effectiveBytes = Decoders.BASE64.decode(secret).length;
        } catch (DecodingException e) {
            effectiveBytes = secret.getBytes(StandardCharsets.UTF_8).length;
        }
        if (effectiveBytes < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 256 bits (32 bytes) for HS256. Generate one with: openssl rand -base64 48");
        }
    }

    private SecretKey resolveSigningKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (DecodingException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public record Token(String value, Instant expiresAt) {
    }
}