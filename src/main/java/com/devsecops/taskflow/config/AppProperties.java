package com.devsecops.taskflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Seed seed) {

    public record Jwt(String secret, long expirationSeconds) {
    }

    public record Cors(String allowedOrigins) {
    }

    public record Seed(boolean enabled, boolean demoEnabled, Admin admin) {
    }

    public record Admin(String username, String password) {
    }
}