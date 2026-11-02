package com.bharti.flashsale.payment.config;

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
    NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(Topics.PAYMENT_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic paymentFailedTopic() {
        return TopicBuilder.name(Topics.PAYMENT_FAILED).partitions(3).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }
}
