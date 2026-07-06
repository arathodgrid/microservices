package com.example.loadbalancer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/loadbalancer")
public class LoadBalancerController {

    private final WebClient.Builder webClientBuilder;

    public LoadBalancerController(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @GetMapping("/users")
    public Mono<String> getUsers() {
        return webClientBuilder.build()
                .get()
                .uri("http://user-service/users")
                .retrieve()
                .bodyToMono(String.class);
    }
}
