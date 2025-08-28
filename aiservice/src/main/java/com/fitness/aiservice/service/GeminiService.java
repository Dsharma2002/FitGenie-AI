package com.fitness.aiservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeminiService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String getAnswer(String prompt) {
        // Map<String, Object> requestBody = Map.of(
        //         "contents", new Object[]{
        //             Map.of("parts", new Object[]{
        //         Map.of("test", prompt)
        //     })
        //         }
        // );
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        try {
            return webClient.post()
                    .uri(geminiApiUrl + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            // Log the real error payload from Google — this tells you exactly what’s wrong
            log.error("Gemini error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
        // String response = webClient.post()
        //         .uri(geminiApiUrl + geminiApiKey)
        //         .header("Content-Type", "application/json")
        //         .bodyValue(requestBody)
        //         .retrieve()
        //         .bodyToMono(String.class)
        //         .block();

        // return response;
    }
}
