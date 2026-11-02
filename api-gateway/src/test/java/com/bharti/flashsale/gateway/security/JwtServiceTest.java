package com.bharti.flashsale.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(new SecurityProperties(
            new SecurityProperties.Jwt("test-secret-key-that-is-long-enough-1234567890", 60),
            Map.of()));

    @Test
    void generatedTokenContainsSubjectAndRoles() {
        String token = jwtService.generateToken("alice", List.of("USER"));

        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get("roles", List.class)).containsExactly("USER");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken("alice", List.of("USER"));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService other = new JwtService(new SecurityProperties(
                new SecurityProperties.Jwt("a-completely-different-secret-key-0987654321", 60), Map.of()));
        String foreignToken = other.generateToken("mallory", List.of("ADMIN"));

        assertThatThrownBy(() -> jwtService.parse(foreignToken)).isInstanceOf(JwtException.class);
    }
}
