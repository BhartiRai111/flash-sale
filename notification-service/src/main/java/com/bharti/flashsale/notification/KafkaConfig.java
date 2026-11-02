package com.bharti.flashsale.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import com.fasterxml.jackson.core.JsonProcessingException;

@Configuration
public class KafkaConfig {

    /** SMTP outages are usually short: back off exponentially (1s, 2s, 4s...) for up to ~30s, then DLT. */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(30_000L);
        DefaultErrorHandler handler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), backOff);
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }
}
