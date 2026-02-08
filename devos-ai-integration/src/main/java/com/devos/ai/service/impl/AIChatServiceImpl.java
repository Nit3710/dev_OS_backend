package com.devos.ai.service.impl;

import com.devos.ai.service.AIProviderFactory;
import com.devos.ai.service.AIService;
import com.devos.ai.service.AIChatService;
import com.devos.core.domain.entity.AIMessage;
import com.devos.core.domain.entity.LLMProvider;
import com.devos.core.domain.entity.Project;
import com.devos.core.domain.entity.User;
import com.devos.core.repository.AIMessageRepository;
import com.devos.core.repository.LLMProviderRepository;
import com.devos.core.repository.ProjectRepository;
import com.devos.core.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatServiceImpl implements AIChatService {

    private final AIProviderFactory aiProviderFactory;
    private final AIMessageRepository aiMessageRepository;
    private final ProjectRepository projectRepository;
    private final LLMProviderRepository llmProviderRepository;
    private final AuthService authService;

    @Override
    public AIMessage sendMessage(Long projectId, String content, String threadId, Long llmProviderId,
                                Map<String, Object> context, Integer maxTokens, Double temperature, String token) {
        
        User user = authService.getCurrentUser(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Save user message
        AIMessage userMessage = AIMessage.builder()
                .type(AIMessage.MessageType.USER)
                .content(content)
                .project(project)
                .threadId(threadId != null ? threadId : UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
        aiMessageRepository.save(userMessage);

        // Get LLM provider
        LLMProvider provider = getLLMProvider(llmProviderId, user);
        AIService aiService = aiProviderFactory.getService(provider);

        // Prepare AI options
        Map<String, Object> options = new HashMap<>();
        options.put("model", provider.getModelName());
        options.put("max_tokens", maxTokens != null ? maxTokens : provider.getMaxTokens());
        options.put("temperature", temperature != null ? temperature : provider.getTemperature());
        if (context != null) {
            options.putAll(context);
        }

        // Generate AI response
        long startTime = System.currentTimeMillis();
        try {
            String response = aiService.generateResponse(content, options).block();
            long processingTime = System.currentTimeMillis() - startTime;

            // Calculate token count and cost
            int tokenCount = aiService.countTokens(content + response).block();
            double cost = aiService.calculateCost(content, response, provider.getModelName()).block();

            // Save AI response
            AIMessage aiResponse = AIMessage.builder()
                    .type(AIMessage.MessageType.ASSISTANT)
                    .content(response)
                    .project(project)
                    .llmProvider(provider)
                    .threadId(userMessage.getThreadId())
                    .parentMessageId(userMessage.getId())
                    .tokenCount(tokenCount)
                    .processingTimeMs(processingTime)
                    .cost(cost)
                    .modelUsed(provider.getModelName())
                    .createdAt(LocalDateTime.now())
                    .build();

            // Update provider usage
            provider.setTokensUsed(provider.getTokensUsed() + tokenCount);
            provider.setCurrentCost(provider.getCurrentCost() + cost);
            llmProviderRepository.save(provider);

            return aiMessageRepository.save(aiResponse);

        } catch (Exception e) {
            log.error("Error generating AI response", e);
            
            // Save error message
            AIMessage errorMessage = AIMessage.builder()
                    .type(AIMessage.MessageType.ERROR)
                    .content("Error generating AI response: " + e.getMessage())
                    .project(project)
                    .llmProvider(provider)
                    .threadId(userMessage.getThreadId())
                    .parentMessageId(userMessage.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            
            return aiMessageRepository.save(errorMessage);
        }
    }

    @Override
    public SseEmitter streamMessage(Long projectId, String message, String threadId, Long llmProviderId, String token) {
        User user = authService.getCurrentUser(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        SseEmitter emitter = new SseEmitter(30000L);
        String streamId = UUID.randomUUID().toString();
        String actualThreadId = threadId != null ? threadId : UUID.randomUUID().toString();

        // Save user message
        AIMessage userMessage = AIMessage.builder()
                .type(AIMessage.MessageType.USER)
                .content(message)
                .project(project)
                .threadId(actualThreadId)
                .createdAt(LocalDateTime.now())
                .build();
        aiMessageRepository.save(userMessage);

        // Get LLM provider
        LLMProvider provider = getLLMProvider(llmProviderId, user);
        AIService aiService = aiProviderFactory.getService(provider);

        // Prepare AI options
        Map<String, Object> options = new HashMap<>();
        options.put("model", provider.getModelName());
        options.put("max_tokens", provider.getMaxTokens());
        options.put("temperature", provider.getTemperature());

        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder responseBuilder = new StringBuilder();
                long startTime = System.currentTimeMillis();

                aiService.generateStreamResponse(message, options)
                        .doOnComplete(() -> {
                            long processingTime = System.currentTimeMillis() - startTime;
                            String fullResponse = responseBuilder.toString();
                            
                            // Calculate token count and cost
                            int tokenCount = aiService.countTokens(message + fullResponse).block();
                            double cost = aiService.calculateCost(message, fullResponse, provider.getModelName()).block();

                            // Save complete AI response
                            AIMessage aiResponse = AIMessage.builder()
                                    .type(AIMessage.MessageType.ASSISTANT)
                                    .content(fullResponse)
                                    .project(project)
                                    .llmProvider(provider)
                                    .threadId(actualThreadId)
                                    .parentMessageId(userMessage.getId())
                                    .tokenCount(tokenCount)
                                    .processingTimeMs(processingTime)
                                    .cost(cost)
                                    .modelUsed(provider.getModelName())
                                    .isStreaming(true)
                                    .streamId(streamId)
                                    .createdAt(LocalDateTime.now())
                                    .build();

                            aiMessageRepository.save(aiResponse);

                            // Update provider usage
                            provider.setTokensUsed(provider.getTokensUsed() + tokenCount);
                            provider.setCurrentCost(provider.getCurrentCost() + cost);
                            llmProviderRepository.save(provider);

                            emitter.complete();
                        })
                        .doOnError(error -> {
                            log.error("Error in streaming response", error);
                            emitter.completeWithError(error);
                        })
                        .subscribe(chunk -> {
                            responseBuilder.append(chunk);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(chunk));
                            } catch (Exception e) {
                                log.error("Error sending SSE event", e);
                                emitter.completeWithError(e);
                            }
                        });

            } catch (Exception e) {
                log.error("Error setting up stream", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Override
    public Page<AIMessage> getMessages(Long projectId, Pageable pageable, String token) {
        User user = authService.getCurrentUser(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify user has access to project
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return aiMessageRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
    }

    @Override
    public List<AIMessage> getThreadMessages(Long projectId, String threadId, String token) {
        User user = authService.getCurrentUser(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify user has access to project
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return aiMessageRepository.findByProjectIdAndThreadIdOrderByCreatedAt(projectId, threadId);
    }

    @Override
    public void deleteMessage(Long messageId, String token) {
        User user = authService.getCurrentUser(token);
        AIMessage message = aiMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Verify user has access to project
        if (!message.getProject().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        aiMessageRepository.delete(message);
    }

    @Override
    public Object getUsageStats(Long projectId, String token) {
        User user = authService.getCurrentUser(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify user has access to project
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", aiMessageRepository.countByProjectAndType(projectId, AIMessage.MessageType.ASSISTANT));
        stats.put("totalTokens", aiMessageRepository.getTotalTokensUsed(projectId));
        stats.put("totalCost", aiMessageRepository.getTotalCost(projectId));
        stats.put("userMessages", aiMessageRepository.countByProjectAndType(projectId, AIMessage.MessageType.USER));

        return stats;
    }

    @Override
    public void clearChat(Long projectId, String threadId, String token) {
        User user = authService.getCurrentUser(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify user has access to project
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (threadId != null) {
            // Clear specific thread
            List<AIMessage> messages = aiMessageRepository.findByProjectIdAndThreadIdOrderByCreatedAt(projectId, threadId);
            aiMessageRepository.deleteAll(messages);
        } else {
            // Clear all messages for project
            List<AIMessage> messages = aiMessageRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
            aiMessageRepository.deleteAll(messages);
        }
    }

    private LLMProvider getLLMProvider(Long llmProviderId, User user) {
        if (llmProviderId != null) {
            return llmProviderRepository.findById(llmProviderId)
                    .orElseThrow(() -> new RuntimeException("LLM provider not found"));
        } else {
            // Use default provider
            return llmProviderRepository.findByUserAndIsDefaultTrue(user)
                    .orElseThrow(() -> new RuntimeException("No default LLM provider configured"));
        }
    }
}
