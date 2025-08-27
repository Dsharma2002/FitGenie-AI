package com.fitness.activityservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {

    private final WebClient userServiceWebClient;

    public boolean validateUser(String id) {
        log.info("Calling User Validation API for userId: {}", id);
        return userServiceWebClient.get()
                .uri("/api/users/{id}/validate", id)
                .retrieve()
                .bodyToMono(Boolean.class)
                // 404 -> false
                .onErrorResume(WebClientResponseException.NotFound.class,
                        _ -> Mono.just(false))
                // 400 -> IllegalArgumentException
                .onErrorMap(WebClientResponseException.BadRequest.class,
                        e -> new IllegalArgumentException("Invalid request: " + id, e))
                // network/timeout/etc. -> IllegalStateException
                .onErrorMap(WebClientRequestException.class,
                        e -> new IllegalStateException("USER-SERVICE unreachable", e))
                // body empty -> false (avoids null)
                .blockOptional()
                .orElse(false);
    }
}
