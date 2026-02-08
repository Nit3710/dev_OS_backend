package com.devos.api.controller;

import com.devos.api.dto.AIMessageDto;
import com.devos.api.dto.ChatRequest;
import com.devos.core.domain.entity.AIMessage;
import com.devos.ai.service.AIChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIChatService aiChatService;

    @PostMapping("/chat/{projectId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<AIMessageDto> sendMessage(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @Valid @RequestBody ChatRequest chatRequest) {
        
        String jwtToken = token.replace("Bearer ", "");
        
        AIMessage response = aiChatService.sendMessage(
            projectId,
            chatRequest.getContent(),
            chatRequest.getThreadId(),
            chatRequest.getLlmProviderId(),
            chatRequest.getContext(),
            chatRequest.getMaxTokens(),
            chatRequest.getTemperature(),
            jwtToken
        );
        
        log.info("AI message sent for project: {}", projectId);
        return ResponseEntity.ok(AIMessageDto.from(response));
    }

    @GetMapping(value = "/chat/{projectId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public SseEmitter streamChat(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam String message,
            @RequestParam(required = false) String threadId,
            @RequestParam(required = false) Long llmProviderId) {
        
        String jwtToken = token.replace("Bearer ", "");
        
        return aiChatService.streamMessage(
            projectId,
            message,
            threadId,
            llmProviderId,
            jwtToken
        );
    }

    @GetMapping("/messages/{projectId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Page<AIMessageDto>> getMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        String jwtToken = token.replace("Bearer ", "");
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AIMessage> messages = aiChatService.getMessages(projectId, pageable, jwtToken);
        Page<AIMessageDto> messageDtos = messages.map(AIMessageDto::from);
        
        return ResponseEntity.ok(messageDtos);
    }

    @GetMapping("/messages/{projectId}/thread/{threadId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<List<AIMessageDto>> getThreadMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @PathVariable String threadId) {
        
        String jwtToken = token.replace("Bearer ", "");
        
        List<AIMessage> messages = aiChatService.getThreadMessages(projectId, threadId, jwtToken);
        List<AIMessageDto> messageDtos = messages.stream()
                .map(AIMessageDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(messageDtos);
    }

    @DeleteMapping("/messages/{messageId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMessage(
            @RequestHeader("Authorization") String token,
            @PathVariable Long messageId) {
        
        String jwtToken = token.replace("Bearer ", "");
        aiChatService.deleteMessage(messageId, jwtToken);
        
        log.info("AI message deleted: {}", messageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usage/{projectId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Object> getUsageStats(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId) {
        
        String jwtToken = token.replace("Bearer ", "");
        Object usageStats = aiChatService.getUsageStats(projectId, jwtToken);
        
        return ResponseEntity.ok(usageStats);
    }

    @PostMapping("/clear/{projectId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> clearChat(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam(required = false) String threadId) {
        
        String jwtToken = token.replace("Bearer ", "");
        aiChatService.clearChat(projectId, threadId, jwtToken);
        
        log.info("Chat cleared for project: {}", projectId);
        return ResponseEntity.ok().build();
    }
}
