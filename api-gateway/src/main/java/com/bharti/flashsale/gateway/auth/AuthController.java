package com.bharti.flashsale.gateway.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bharti.flashsale.gateway.security.JwtService;
import com.bharti.flashsale.gateway.security.SecurityProperties;
import com.bharti.flashsale.gateway.security.SecurityProperties.DemoUser;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/** Demo login that issues JWTs for the users configured in application.yml. */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SecurityProperties properties;
    private final JwtService jwtService;

    public record LoginRequest(String username, String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }

    @PostMapping("/login")
    public Mono<TokenResponse> login(@RequestBody LoginRequest request) {
        DemoUser user = request.username() == null ? null : properties.demoUsers().get(request.username());
        if (user == null || !user.password().equals(request.password())) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        }
        String token = jwtService.generateToken(request.username(), user.roles());
        return Mono.just(new TokenResponse(token, "Bearer", jwtService.expiresInSeconds()));
    }
}
