package com.bharti.flashsale.inventory.config;

import java.time.Duration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.bharti.flashsale.common.Topics;
import com.bharti.flashsale.inventory.service.ProductService;
import com.bharti.flashsale.inventory.web.ProductDtos.ProductResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class InventoryConfig {

    /** Distributed lock backed by Redis; a lock auto-expires after 10s if an instance dies while holding it. */
    @Bean
    RedisLockRegistry redisLockRegistry(RedisConnectionFactory connectionFactory) {
        return new RedisLockRegistry(connectionFactory, "inventory-locks", 10_000L);
    }

    /** Product cache stored as readable JSON with a 10-minute TTL. */
    @Bean
    RedisCacheManagerBuilderCustomizer productCacheCustomizer(ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<ProductResponse> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, ProductResponse.class);
        return builder -> builder.withCacheConfiguration(ProductService.PRODUCT_CACHE,
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .disableCachingNullValues()
                        .serializeValuesWith(SerializationPair.fromSerializer(serializer)));
    }

    @Bean
    NewTopic inventoryReservedTopic() {
        return TopicBuilder.name(Topics.INVENTORY_RESERVED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic inventoryFailedTopic() {
        return TopicBuilder.name(Topics.INVENTORY_FAILED).partitions(3).replicas(1).build();
    }

    /** Retry 3 times (covers lock timeouts), then send to a dead-letter topic. */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }
}
