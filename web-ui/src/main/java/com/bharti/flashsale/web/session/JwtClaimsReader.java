package com.bharti.flashsale.web.session;

import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Reads the roles from the JWT payload only to decide what to SHOW (e.g. the Admin menu).
 * It does not verify the signature: security is enforced by the gateway and the services,
 * so a user who edits the token gains nothing.
 */
@Component
@RequiredArgsConstructor
public class JwtClaimsReader {

    private final ObjectMapper objectMapper;

    public List<String> roles(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode roles = objectMapper.readTree(payload).path("roles");
            if (!roles.isArray()) {
                return List.of();
            }
            return objectMapper.convertValue(roles,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }
}
