package com.bharti.flashsale.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bharti.flashsale.web.client.ApiException;
import com.bharti.flashsale.web.client.ApiModels.OrderView;
import com.bharti.flashsale.web.client.ApiModels.ProductView;
import com.bharti.flashsale.web.client.GatewayClient;
import com.bharti.flashsale.web.session.JwtClaimsReader;
import com.bharti.flashsale.web.session.LoginUser;

/** The UI is tested with the gateway mocked, so no other service needs to run. */
@SpringBootTest
@AutoConfigureMockMvc
class WebUiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtClaimsReader jwtClaimsReader;

    @MockitoBean
    GatewayClient gatewayClient;

    private final ProductView phone = new ProductView(1L, "Flash Deal Smartphone", new BigDecimal("14999.00"), 7);

    private MockHttpSession loggedIn(String username, List<String> roles) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(username, "token-123", roles));
        return session;
    }

    @Test
    void productListIsPublic() throws Exception {
        when(gatewayClient.listProducts()).thenReturn(List.of(phone));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Flash Deal Smartphone")))
                .andExpect(content().string(Matchers.containsString("Only 7 left")));
    }

    @Test
    void ordersPageNeedsLogin() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void buyingSendsTheIdempotencyKeyAndRedirectsToTheOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(gatewayClient.placeOrder(eq("token-123"), eq("key-1"), eq(1L), eq(2), eq("alice@example.com")))
                .thenReturn(new OrderView(orderId, "alice", 1L, 2, phone.price(), new BigDecimal("29998.00"),
                        "PENDING", null, Instant.now(), Instant.now()));

        mockMvc.perform(post("/products/1/buy")
                        .session(loggedIn("alice", List.of("USER")))
                        .param("idempotencyKey", "key-1")
                        .param("quantity", "2")
                        .param("customerEmail", "alice@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/" + orderId));

        verify(gatewayClient).placeOrder("token-123", "key-1", 1L, 2, "alice@example.com");
    }

    @Test
    void soldOutShowsTheMessageOnTheProductPage() throws Exception {
        when(gatewayClient.placeOrder(anyString(), anyString(), anyLong(), anyInt(), anyString()))
                .thenThrow(new ApiException(409, "Product 1 is sold out"));

        mockMvc.perform(post("/products/1/buy")
                        .session(loggedIn("alice", List.of("USER")))
                        .param("idempotencyKey", "key-2")
                        .param("quantity", "1")
                        .param("customerEmail", "alice@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/1"));
    }

    @Test
    void pendingOrderPageAutoRefreshes() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(gatewayClient.getOrder("token-123", orderId)).thenReturn(new OrderView(orderId, "alice", 1L, 1,
                phone.price(), phone.price(), "PENDING", null, Instant.now(), Instant.now()));
        when(gatewayClient.listProducts()).thenReturn(List.of(phone));

        mockMvc.perform(get("/orders/" + orderId).session(loggedIn("alice", List.of("USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("http-equiv=\"refresh\"")));
    }

    @Test
    void adminPageIsForbiddenForNormalUsers() throws Exception {
        mockMvc.perform(get("/admin/products").session(loggedIn("alice", List.of("USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolesAreReadFromTheTokenPayload() {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin\",\"roles\":[\"ADMIN\",\"USER\"]}".getBytes(StandardCharsets.UTF_8));
        String fakeJwt = "header." + payload + ".signature";

        assertThat(jwtClaimsReader.roles(fakeJwt)).containsExactly("ADMIN", "USER");
        assertThat(jwtClaimsReader.roles("not-a-jwt")).isEmpty();
    }
}
