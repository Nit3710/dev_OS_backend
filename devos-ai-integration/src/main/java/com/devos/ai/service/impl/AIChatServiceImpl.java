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
@Slf4j
public class AIChatServiceImpl implements AIChatService {

    private final AIProviderFactory aiProviderFactory;
    private final AIMessageRepository aiMessageRepository;
    private final ProjectRepository projectRepository;
    private final LLMProviderRepository llmProviderRepository;
    private final AuthService authService;
    private final com.devos.core.service.FileService fileService;

    public AIChatServiceImpl(
            AIProviderFactory aiProviderFactory,
            AIMessageRepository aiMessageRepository,
            ProjectRepository projectRepository,
            LLMProviderRepository llmProviderRepository,
            AuthService authService,
            @org.springframework.beans.factory.annotation.Qualifier("coreFileServiceImpl") com.devos.core.service.FileService fileService) {
        this.aiProviderFactory = aiProviderFactory;
        this.aiMessageRepository = aiMessageRepository;
        this.projectRepository = projectRepository;
        this.llmProviderRepository = llmProviderRepository;
        this.authService = authService;
        this.fileService = fileService;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public AIMessage sendMessage(Long projectId, String content, String threadId, Long llmProviderId,
                                Map<String, Object> context, Integer maxTokens, Double temperature) {
        
        User user = authService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new com.devos.core.exception.ProjectNotFoundException(projectId));

        // Verify user has access to project
        if (!project.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to project " + projectId);
        }

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
            String enhancedPrompt = buildEnhancedPrompt(project, content, context);
            String response = aiService.generateResponse(enhancedPrompt, options).block();
            long processingTime = System.currentTimeMillis() - startTime;

            // Calculate token count and cost
            int tokenCount = aiService.countTokens(enhancedPrompt + response).block();
            double cost = aiService.calculateCost(enhancedPrompt, response, provider.getModelName()).block();

            // Parse changes if present
            Map<String, Object> metadata = parseCodeChanges(response);

            // Save AI response
            AIMessage aiResponse = AIMessage.builder()
                    .type(AIMessage.MessageType.ASSISTANT)
                    .content(response)
                    .metadata(metadata)
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
    @org.springframework.transaction.annotation.Transactional
    public SseEmitter streamMessage(Long projectId, String message, String threadId, Long llmProviderId, Map<String, Object> context) {
        User user = authService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new com.devos.core.exception.ProjectNotFoundException(projectId));

        // Verify user has access to project
        if (!project.getUser().getId().equals(user.getId()) && user.getRole() != com.devos.core.domain.entity.User.UserRole.ADMIN) {
            log.warn("ACCESS DENIED: User {} (role: {}) attempted to access project {} owned by user {}", 
                    user.getId(), user.getRole(), projectId, project.getUser().getId());
            throw new org.springframework.security.access.AccessDeniedException("Access denied to project " + projectId);
        }

        SseEmitter emitter = new SseEmitter(60000L); // Increased timeout for long AI generations
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
                String enhancedPrompt = buildEnhancedPrompt(project, message, context);

                aiService.generateStreamResponse(enhancedPrompt, options)
                        .doOnComplete(() -> {
                            long processingTime = System.currentTimeMillis() - startTime;
                            String fullResponse = responseBuilder.toString();
                            
                            // Calculate token count and cost
                            int tokenCount = aiService.countTokens(enhancedPrompt + fullResponse).block();
                            double cost = aiService.calculateCost(enhancedPrompt, fullResponse, provider.getModelName()).block();

                            // Parse changes if present
                            Map<String, Object> metadata = parseCodeChanges(fullResponse);

                            // Save complete AI response
                            AIMessage aiResponse = AIMessage.builder()
                                    .type(AIMessage.MessageType.ASSISTANT)
                                    .content(fullResponse)
                                    .metadata(metadata)
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<AIMessage> getMessages(Long projectId, Pageable pageable) {
        User user = authService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new com.devos.core.exception.ProjectNotFoundException(projectId));

        // Verify user has access to project
        if (!project.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to project " + projectId);
        }

        return aiMessageRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<AIMessage> getThreadMessages(Long projectId, String threadId) {
        User user = authService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new com.devos.core.exception.ProjectNotFoundException(projectId));

        // Verify user has access to project
        if (!project.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to project " + projectId);
        }

        return aiMessageRepository.findByProjectIdAndThreadIdOrderByCreatedAt(projectId, threadId);
    }

    @Override
    public void deleteMessage(Long messageId) {
        User user = authService.getCurrentUser();
        AIMessage message = aiMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Verify user has access to project
        if (!message.getProject().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        aiMessageRepository.delete(message);
    }

    @Override
    public Object getUsageStats(Long projectId) {
        User user = authService.getCurrentUser();
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
    public void clearChat(Long projectId, String threadId) {
        User user = authService.getCurrentUser();
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

    private Map<String, Object> parseCodeChanges(String response) {
        Map<String, Object> metadata = new HashMap<>();
        try {
            int jsonStart = response.indexOf("```json");
            if (jsonStart != -1) {
                int start = response.indexOf("{", jsonStart);
                int end = response.lastIndexOf("}");
                if (start != -1 && end != -1 && end > start) {
                    String jsonStr = response.substring(start, end + 1);
                    Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonStr, Map.class);
                    metadata.putAll(parsed);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing code changes from AI response", e);
        }
        return metadata;
    }

    private String buildEnhancedPrompt(Project project, String userMessage, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        
        // System Instruction
        prompt.append("You are DevOS AI, an expert software engineer assistant. Your goal is to help the user develop their project.\n");
        prompt.append("GUIDELINES:\n");
        prompt.append("- Be concise and professional.\n");
        prompt.append("- When proposing code changes, use the following JSON block format inside your response:\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"explanation\": \"Brief description of what you're changing\",\n");
        prompt.append("  \"changes\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"file\": \"path/to/file.ext\",\n");
        prompt.append("      \"action\": \"modify\",\n");
        prompt.append("      \"content\": \"FULL CONTENT OF THE FILE AFTER CHANGES\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n");
        prompt.append("- Ensure the 'file' path is relative to the project root: ").append(project.getLocalPath()).append("\n\n");

        // Project Context: File List
        prompt.append("PROJECT STRUCTURE:\n");
        try {
            Map<String, Object> files = fileService.getProjectFiles(project.getId());
            formatFileList(files, "", prompt);
        } catch (Exception e) {
            prompt.append("(Unable to load file list)\n");
        }
        prompt.append("\n");

        // Additional Context (e.g., currently open files)
        if (context != null && context.containsKey("activeFiles")) {
            prompt.append("ACTIVE FILES CONTENT:\n");
            List<String> activeFiles = (List<String>) context.get("activeFiles");
            for (String filePath : activeFiles) {
                try {
                    String content = fileService.getFileContent(project.getId(), filePath);
                    prompt.append("--- FILE: ").append(filePath).append(" ---\n");
                    prompt.append(content).append("\n\n");
                } catch (Exception e) {
                    prompt.append("--- FILE: ").append(filePath).append(" (Error reading) ---\n\n");
                }
            }
        }

        prompt.append("USER REQUEST: ").append(userMessage).append("\n");
        prompt.append("ASSISTANT RESPONSE:");

        return prompt.toString();
    }

    private void formatFileList(Map<String, Object> files, String indent, StringBuilder sb) {
        if (files == null || !files.containsKey("children")) return;
        List<Map<String, Object>> children = (List<Map<String, Object>>) files.get("children");
        for (Map<String, Object> child : children) {
            String name = (String) child.get("name");
            String type = (String) child.get("type");
            sb.append(indent).append("- ").append(name).append(type.equals("directory") ? "/" : "").append("\n");
            if (type.equals("directory")) {
                formatFileList(child, indent + "  ", sb);
            }
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
