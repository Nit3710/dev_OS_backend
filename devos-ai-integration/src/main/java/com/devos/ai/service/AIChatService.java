package com.devos.ai.service;

import com.devos.core.domain.entity.AIMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AIChatService {

    AIMessage sendMessage(Long projectId, String content, String threadId, Long llmProviderId, 
                        Map<String, Object> context, Integer maxTokens, Double temperature, String token);

    SseEmitter streamMessage(Long projectId, String message, String threadId, Long llmProviderId, String token);

    Page<AIMessage> getMessages(Long projectId, Pageable pageable, String token);

    List<AIMessage> getThreadMessages(Long projectId, String threadId, String token);

    void deleteMessage(Long messageId, String token);

    Object getUsageStats(Long projectId, String token);

    void clearChat(Long projectId, String threadId, String token);
}
