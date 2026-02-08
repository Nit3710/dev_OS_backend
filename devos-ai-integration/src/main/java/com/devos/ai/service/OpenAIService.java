package com.devos.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService implements AIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${devos.ai.openai.api-key:}")
    private String apiKey;

    @Value("${devos.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${devos.ai.openai.default-model:gpt-3.5-turbo}")
    private String defaultModel;

    @Value("${devos.ai.timeout:30000}")
    private int timeout;

    @Override
    public Mono<String> generateResponse(String prompt, Map<String, Object> options) {
        Map<String, Object> requestBody = buildRequestBody(prompt, options);
        
        return webClient.post()
                .uri(baseUrl + "/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .map(this::extractContentFromResponse)
                .doOnError(error -> log.error("Error generating OpenAI response", error));
    }

    @Override
    public Flux<String> generateStreamResponse(String prompt, Map<String, Object> options) {
        Map<String, Object> requestBody = buildRequestBody(prompt, options);
        requestBody.put("stream", true);
        
        return webClient.post()
                .uri(baseUrl + "/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(timeout))
                .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
                .map(line -> line.substring(6))
                .map(this::extractContentFromStreamResponse)
                .doOnError(error -> log.error("Error in OpenAI stream response", error));
    }

    @Override
    public Mono<Integer> countTokens(String text) {
        // Simple estimation - in production, you'd use tiktoken or similar
        return Mono.just(text.length() / 4);
    }

    @Override
    public Mono<Double> calculateCost(String prompt, String response, String model) {
        // Pricing per 1K tokens (example rates for GPT-3.5-turbo)
        Map<String, Map<String, Double>> pricing = Map.of(
            "gpt-3.5-turbo", Map.of(
                "input", 0.0015,
                "output", 0.002
            ),
            "gpt-4", Map.of(
                "input", 0.03,
                "output", 0.06
            )
        );
        
        return countTokens(prompt)
            .zipWith(countTokens(response))
            .map(tuple -> {
                int promptTokens = tuple.getT1();
                int responseTokens = tuple.getT2();
                
                Map<String, Double> modelPricing = pricing.getOrDefault(model, pricing.get("gpt-3.5-turbo"));
                double inputCost = (promptTokens / 1000.0) * modelPricing.get("input");
                double outputCost = (responseTokens / 1000.0) * modelPricing.get("output");
                
                return inputCost + outputCost;
            });
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public String getDefaultModel() {
        return defaultModel;
    }

    @Override
    public Map<String, Object> getCapabilities() {
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("streaming", true);
        capabilities.put("max_tokens", 4096);
        capabilities.put("models", java.util.List.of("gpt-3.5-turbo", "gpt-4", "gpt-4-turbo-preview"));
        capabilities.put("supports_functions", true);
        capabilities.put("supports_vision", false);
        return capabilities;
    }

    private Map<String, Object> buildRequestBody(String prompt, Map<String, Object> options) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", options.getOrDefault("model", defaultModel));
        body.put("messages", java.util.List.of(
            Map.of("role", "user", "content", prompt)
        ));
        body.put("max_tokens", options.getOrDefault("max_tokens", 1000));
        body.put("temperature", options.getOrDefault("temperature", 0.7));
        
        if (options.containsKey("top_p")) {
            body.put("top_p", options.get("top_p"));
        }
        if (options.containsKey("frequency_penalty")) {
            body.put("frequency_penalty", options.get("frequency_penalty"));
        }
        if (options.containsKey("presence_penalty")) {
            body.put("presence_penalty", options.get("presence_penalty"));
        }
        
        return body;
    }

    private String extractContentFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    return content != null ? content.asText() : "";
                }
            }
        } catch (Exception e) {
            log.error("Error parsing OpenAI response", e);
        }
        return "";
    }

    private String extractContentFromStreamResponse(String jsonLine) {
        try {
            JsonNode root = objectMapper.readTree(jsonLine);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).get("delta");
                if (delta != null) {
                    JsonNode content = delta.get("content");
                    return content != null ? content.asText() : "";
                }
            }
        } catch (Exception e) {
            log.debug("Error parsing OpenAI stream response: {}", jsonLine, e);
        }
        return "";
    }
}
