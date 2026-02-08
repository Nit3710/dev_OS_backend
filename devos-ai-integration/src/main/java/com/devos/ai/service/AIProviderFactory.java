package com.devos.ai.service;

import com.devos.core.domain.entity.LLMProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderFactory {

    private final List<AIService> aiServices;
    private final Map<LLMProvider.ProviderType, AIService> serviceMap = new HashMap<>();
    public AIService getService(LLMProvider.ProviderType providerType) {
        if (serviceMap.isEmpty()) {
            initializeServiceMap();
        }
        return serviceMap.get(providerType);
    }

    public AIService getService(LLMProvider provider) {
        AIService service = getService(provider.getType());
        if (service == null) {
            throw new IllegalArgumentException("Unsupported AI provider: " + provider.getType());
        }
        return service;
    }

    public Map<String, Object> getProviderCapabilities(LLMProvider.ProviderType providerType) {
        AIService service = getService(providerType);
        return service != null ? service.getCapabilities() : Map.of();
    }

    public List<String> getSupportedModels(LLMProvider.ProviderType providerType) {
        Map<String, Object> capabilities = getProviderCapabilities(providerType);
        @SuppressWarnings("unchecked")
        List<String> models = (List<String>) capabilities.get("models");
        return models != null ? models : List.of();
    }

    public boolean supportsStreaming(LLMProvider.ProviderType providerType) {
        AIService service = getService(providerType);
        return service != null && service.supportsStreaming();
    }

    private void initializeServiceMap() {
        for (AIService service : aiServices) {
            if (service instanceof OpenAIService) {
                serviceMap.put(LLMProvider.ProviderType.OPENAI, service);
            } else if (service instanceof AnthropicService) {
                serviceMap.put(LLMProvider.ProviderType.ANTHROPIC, service);
            }
            // Add more providers as needed
        }
        log.info("Initialized AI provider services: {}", serviceMap.keySet());
    }
}
