package com.example.resilience.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/resilience")
public class ResilienceController {

    private final WebClient.Builder webClientBuilder;

    public ResilienceController(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @GetMapping("/user/{id}")
    @CircuitBreaker(name = "resilienceCircuitBreaker", fallbackMethod = "getUserFallback")
    public Mono<String> getUser(@PathVariable Long id) {
        return webClientBuilder.build()
                .get()
                .uri("http://user-service/users/" + id)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getUserFallback(Long id, Throwable throwable) {
        return Mono.just("User service is currently unavailable. (Circuit Breaker Triggered - Fallback returned)");
    }

    @GetMapping("/retry-user/{id}")
    @Retry(name = "resilienceRetry", fallbackMethod = "getUserFallback")
    public Mono<String> retryGetUser(@PathVariable Long id) {
        return webClientBuilder.build()
                .get()
                .uri("http://user-service/users/" + id)
                .retrieve()
                .bodyToMono(String.class);
    }
}
