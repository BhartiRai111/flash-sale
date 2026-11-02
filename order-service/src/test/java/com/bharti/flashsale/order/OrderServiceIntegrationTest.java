package com.bharti.flashsale.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import com.bharti.flashsale.common.Topics;
import com.bharti.flashsale.common.event.PaymentCompletedEvent;
import com.bharti.flashsale.order.client.ProductClient;
import com.bharti.flashsale.order.client.ProductDto;
import com.bharti.flashsale.order.domain.OrderRepository;
import com.bharti.flashsale.order.domain.OrderStatus;
import com.bharti.flashsale.order.outbox.OutboxRepository;
import com.bharti.flashsale.order.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Runs against a real PostgreSQL and Kafka started by Testcontainers (Docker must be running). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ProductClient productClient;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
        when(productClient.getProduct(anyLong()))
                .thenReturn(new ProductDto(1L, "Flash Deal Smartphone", new BigDecimal("14999.00"), 100));
    }

    @Test
    void sameIdempotencyKeyCreatesOnlyOneOrder() throws Exception {
        String body = """
                {"productId": 1, "quantity": 2, "customerEmail": "alice@example.com"}
                """;

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "alice")
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(29998.00));

        // Same key again (user double-clicked / network retry)
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "alice")
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.count()).isEqualTo(1);

        // The outbox publisher pushes the OrderCreated event to Kafka
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(outboxRepository.countByStatus(OutboxStatus.SENT)).isEqualTo(1));
    }

    @Test
    void invalidRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "alice")
                        .header("Idempotency-Key", "key-bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 1, "quantity": 50, "customerEmail": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paymentCompletedEventConfirmsTheOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "bob")
                        .header("Idempotency-Key", "key-pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 1, "quantity": 1, "customerEmail": "bob@example.com"}
                                """))
                .andExpect(status().isAccepted());
        UUID orderId = orderRepository.findByIdempotencyKey("key-pay").orElseThrow().getId();

        String event = objectMapper.writeValueAsString(
                new PaymentCompletedEvent(orderId, UUID.randomUUID(), new BigDecimal("14999.00"), Instant.now()));
        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, orderId.toString(), event);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                        .isEqualTo(OrderStatus.CONFIRMED));
    }
}
