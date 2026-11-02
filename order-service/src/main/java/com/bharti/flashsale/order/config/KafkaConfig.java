package com.bharti.flashsale.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.bharti.flashsale.common.Topics;
import com.fasterxml.jackson.core.JsonProcessingException;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic orderCreatedTopic() {
        return TopicBuilder.name(Topics.ORDER_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(Topics.ORDER_CONFIRMED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderCancelledTopic() {
        return TopicBuilder.name(Topics.ORDER_CANCELLED).partitions(3).replicas(1).build();
    }

    /**
     * Retry a failing message 3 times (1s apart), then park it in a dead-letter topic
     * so one bad message never blocks the partition. Bad JSON goes straight to the DLT.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }
}
