package com.bharti.flashsale.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/** Returned when a downstream service is down or its circuit breaker is open. */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public Mono<ResponseEntity<ProblemDetail>> fallback(@PathVariable String service) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                service + " is temporarily unavailable. Please try again shortly.");
        problem.setTitle("Service Unavailable");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem));
    }
}
