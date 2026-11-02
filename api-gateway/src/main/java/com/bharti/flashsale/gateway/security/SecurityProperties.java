package com.bharti.flashsale.gateway.security;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from the "security" block in application.yml.
 * Demo users live in config so the project runs without a separate user service.
 * In production you would use a user/auth service or an identity provider instead.
 */
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(Jwt jwt, Map<String, DemoUser> demoUsers) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record DemoUser(String password, List<String> roles) {
    }
}
