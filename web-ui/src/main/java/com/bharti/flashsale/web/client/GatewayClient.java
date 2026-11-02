package com.bharti.flashsale.web.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bharti.flashsale.web.client.ApiModels.OrderView;
import com.bharti.flashsale.web.client.ApiModels.ProductView;
import com.bharti.flashsale.web.client.ApiModels.TokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The UI's only door into the system: every call goes through the API gateway,
 * which checks the JWT, applies rate limits and routes to the right microservice.
 */
@Component
public class GatewayClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GatewayClient(RestClient.Builder builder, ObjectMapper objectMapper,
                         @Value("${gateway.url}") String gatewayUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(8000);
        this.objectMapper = objectMapper;
        this.restClient = builder
                .baseUrl(gatewayUrl)
                .requestFactory(requestFactory)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw toApiException(response);
                })
                .build();
    }

    // ---------------------------------------------------------------- auth

    public TokenResponse login(String username, String password) {
        return restClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password))
                .retrieve()
                .body(TokenResponse.class);
    }

    // ---------------------------------------------------------------- products (public)

    public List<ProductView> listProducts() {
        return restClient.get()
                .uri("/api/products")
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductView>>() {
                });
    }

    public ProductView getProduct(Long productId) {
        return restClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .body(ProductView.class);
    }

    // ---------------------------------------------------------------- orders (logged in)

    /** The idempotency key comes from the form, so a double-click or browser resubmit creates one order only. */
    public OrderView placeOrder(String token, String idempotencyKey, Long productId, int quantity, String email) {
        return restClient.post()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("productId", productId, "quantity", quantity, "customerEmail", email))
                .retrieve()
                .body(OrderView.class);
    }

    public List<OrderView> listOrders(String token) {
        return restClient.get()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderView>>() {
                });
    }

    public OrderView getOrder(String token, UUID orderId) {
        return restClient.get()
                .uri("/api/orders/{id}", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(OrderView.class);
    }

    // ---------------------------------------------------------------- admin

    public ProductView createProduct(String token, String name, BigDecimal price, int stock) {
        return restClient.post()
                .uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name, "price", price, "stock", stock))
                .retrieve()
                .body(ProductView.class);
    }

    public ProductView restock(String token, Long productId, int quantity) {
        return restClient.put()
                .uri("/api/products/{id}/stock", productId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("quantity", quantity))
                .retrieve()
                .body(ProductView.class);
    }

    // ---------------------------------------------------------------- helpers

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Turns an error response into a readable message (uses the ProblemDetail "detail" when present). */
    private ApiException toApiException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String message = switch (status) {
            case 401 -> "Please log in again.";
            case 403 -> "You don't have permission to do that.";
            case 429 -> "Too many requests. Please wait a moment and try again.";
            case 503 -> "The service is busy or down right now. Please try again shortly.";
            default -> "Something went wrong (" + status + ").";
        };
        if (!body.isBlank()) {
            try {
                JsonNode json = objectMapper.readTree(body);
                if (json.hasNonNull("detail")) {
                    message = json.get("detail").asText();
                }
            } catch (IOException ignored) {
                // not JSON: keep the generic message
            }
        }
        return new ApiException(status, message);
    }
}
