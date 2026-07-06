package com.example.library.controller;

import com.example.library.model.Reservation;
import com.example.library.repository.ReservationRepository;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final WebClient.Builder webClientBuilder;
    private final CircuitBreakerFactory circuitBreakerFactory;

    public ReservationController(ReservationRepository reservationRepository,
                                 WebClient.Builder webClientBuilder,
                                 CircuitBreakerFactory circuitBreakerFactory) {
        this.reservationRepository = reservationRepository;
        this.webClientBuilder = webClientBuilder;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody Reservation reservation) {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("libraryCircuitBreaker");

        // Verify User Service (via Eureka load balancer)
        Boolean userExists = circuitBreaker.run(
                () -> webClientBuilder.build().get()
                        .uri("http://user-service/users/" + reservation.getUserId())
                        .retrieve()
                        .toBodilessEntity()
                        .map(response -> response.getStatusCode().is2xxSuccessful())
                        .onErrorReturn(false)
                        .block(),
                throwable -> false // Fallback
        );

        if (!Boolean.TRUE.equals(userExists)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User does not exist or user-service is offline");
        }

        // Verify Book Service
        Boolean bookExists = circuitBreaker.run(
                () -> webClientBuilder.build().get()
                        .uri("http://book-service/books/" + reservation.getBookId())
                        .retrieve()
                        .toBodilessEntity()
                        .map(response -> response.getStatusCode().is2xxSuccessful())
                        .onErrorReturn(false)
                        .block(),
                throwable -> false // Fallback
        );

        if (!Boolean.TRUE.equals(bookExists)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Book does not exist or book-service is offline");
        }

        // Save Reservation
        Reservation savedReservation = reservationRepository.save(reservation);

        // Increment reservations count
        circuitBreaker.run(
                () -> webClientBuilder.build().put()
                        .uri("http://book-service/books/" + reservation.getBookId() + "/increment-reservations")
                        .retrieve()
                        .toBodilessEntity()
                        .map(response -> response.getStatusCode().is2xxSuccessful())
                        .onErrorReturn(false)
                        .block(),
                throwable -> false
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(savedReservation);
    }
}
