package com.bharti.flashsale.gateway.security;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Validates the JWT once at the edge and forwards the identity to downstream services
 * as trusted headers (X-User-Id, X-User-Roles). Downstream services never parse tokens.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private static final List<String> PUBLIC_PREFIXES = List.of("/auth/", "/actuator/", "/fallback/");

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublic(request)) {
            // Strip identity headers so a client can never spoof them
            return chain.filter(exchange.mutate().request(stripIdentityHeaders(request)).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        Claims claims;
        try {
            claims = jwtService.parse(authHeader.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange);
        }

        List<?> roles = claims.get("roles", List.class);
        String rolesValue = roles == null ? "" : String.join(",", roles.stream().map(String::valueOf).toList());

        ServerHttpRequest authenticated = request.mutate()
                .headers(h -> {
                    h.remove(USER_ID_HEADER);
                    h.remove(USER_ROLES_HEADER);
                    h.set(USER_ID_HEADER, claims.getSubject());
                    h.set(USER_ROLES_HEADER, rolesValue);
                })
                .build();

        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getPath().value();
        if (PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }
        // Anyone can browse products; creating/updating products needs a token
        return HttpMethod.GET.equals(request.getMethod()) && path.startsWith("/api/products");
    }

    private ServerHttpRequest stripIdentityHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(h -> {
                    h.remove(USER_ID_HEADER);
                    h.remove(USER_ROLES_HEADER);
                })
                .build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        // Run before route filters (rate limiter needs X-User-Id)
        return -100;
    }
}
