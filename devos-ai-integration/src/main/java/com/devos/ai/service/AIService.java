package com.devos.ai.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface AIService {

    Mono<String> generateResponse(String prompt, Map<String, Object> options);

    Flux<String> generateStreamResponse(String prompt, Map<String, Object> options);

    Mono<Integer> countTokens(String text);

    Mono<Double> calculateCost(String prompt, String response, String model);

    boolean supportsStreaming();

    String getDefaultModel();

    Map<String, Object> getCapabilities();
}
