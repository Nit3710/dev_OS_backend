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
public class AnthropicService implements AIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${devos.ai.anthropic.api-key:}")
    private String apiKey;

    @Value("${devos.ai.anthropic.base-url:https://api.anthropic.com}")
    private String baseUrl;

    @Value("${devos.ai.anthropic.default-model:claude-3-sonnet-20240229}")
    private String defaultModel;

    @Value("${devos.ai.timeout:30000}")
    private int timeout;

    @Override
    public Mono<String> generateResponse(String prompt, Map<String, Object> options) {
        Map<String, Object> requestBody = buildRequestBody(prompt, options);
        
        return webClient.post()
                .uri(baseUrl + "/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .map(this::extractContentFromResponse)
                .doOnError(error -> log.error("Error generating Anthropic response", error));
    }

    @Override
    public Flux<String> generateStreamResponse(String prompt, Map<String, Object> options) {
        Map<String, Object> requestBody = buildRequestBody(prompt, options);
        requestBody.put("stream", true);
        
        return webClient.post()
                .uri(baseUrl + "/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(timeout))
                .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
                .map(line -> line.substring(6))
                .map(this::extractContentFromStreamResponse)
                .doOnError(error -> log.error("Error in Anthropic stream response", error));
    }

    @Override
    public Mono<Integer> countTokens(String text) {
        // Simple estimation - Anthropic uses different tokenization
        return Mono.just(text.length() / 4);
    }

    @Override
    public Mono<Double> calculateCost(String prompt, String response, String model) {
        // Pricing per 1K tokens (example rates for Claude)
        Map<String, Map<String, Double>> pricing = Map.of(
            "claude-3-sonnet-20240229", Map.of(
                "input", 0.003,
                "output", 0.015
            ),
            "claude-3-opus-20240229", Map.of(
                "input", 0.015,
                "output", 0.075
            ),
            "claude-3-haiku-20240307", Map.of(
                "input", 0.00025,
                "output", 0.00125
            )
        );
        
        return countTokens(prompt)
            .zipWith(countTokens(response))
            .map(tuple -> {
                int promptTokens = tuple.getT1();
                int responseTokens = tuple.getT2();
                
                Map<String, Double> modelPricing = pricing.getOrDefault(model, pricing.get("claude-3-sonnet-20240229"));
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
        capabilities.put("models", java.util.List.of(
            "claude-3-opus-20240229", 
            "claude-3-sonnet-20240229", 
            "claude-3-haiku-20240307"
        ));
        capabilities.put("supports_functions", false);
        capabilities.put("supports_vision", true);
        capabilities.put("max_context", 200000);
        return capabilities;
    }

    private Map<String, Object> buildRequestBody(String prompt, Map<String, Object> options) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", options.getOrDefault("model", defaultModel));
        body.put("max_tokens", options.getOrDefault("max_tokens", 1000));
        body.put("messages", java.util.List.of(
            Map.of("role", "user", "content", prompt)
        ));
        
        if (options.containsKey("temperature")) {
            body.put("temperature", options.get("temperature"));
        }
        if (options.containsKey("top_p")) {
            body.put("top_p", options.get("top_p"));
        }
        if (options.containsKey("top_k")) {
            body.put("top_k", options.get("top_k"));
        }
        
        return body;
    }

    private String extractContentFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.get("content");
            if (content != null && content.isArray() && content.size() > 0) {
                JsonNode textNode = content.get(0).get("text");
                return textNode != null ? textNode.asText() : "";
            }
        } catch (Exception e) {
            log.error("Error parsing Anthropic response", e);
        }
        return "";
    }

    private String extractContentFromStreamResponse(String jsonLine) {
        try {
            JsonNode root = objectMapper.readTree(jsonLine);
            JsonNode type = root.get("type");
            if (type != null && "content_block_delta".equals(type.asText())) {
                JsonNode delta = root.get("delta");
                if (delta != null) {
                    JsonNode text = delta.get("text");
                    return text != null ? text.asText() : "";
                }
            }
        } catch (Exception e) {
            log.debug("Error parsing Anthropic stream response: {}", jsonLine, e);
        }
        return "";
    }
}
