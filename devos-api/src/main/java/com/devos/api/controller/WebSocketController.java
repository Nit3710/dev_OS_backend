package com.devos.api.controller;

import com.devos.ai.service.AIChatService;
import com.devos.core.service.ActionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final AIChatService aiChatService;
    private final ActionPlanService actionPlanService;

    @MessageMapping("/chat/{projectId}")
    @SendToUser("/queue/chat/{projectId}")
    public void handleChatMessage(
            @DestinationVariable Long projectId,
            @Payload Map<String, Object> message,
            Principal principal) {
        
        try {
            String content = (String) message.get("content");
            String threadId = (String) message.get("threadId");
            Long llmProviderId = message.get("llmProviderId") != null ? 
                ((Number) message.get("llmProviderId")).longValue() : null;
            
            // Send user message confirmation
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/chat/" + projectId,
                Map.of(
                    "type", "user_message",
                    "content", content,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // Process AI response asynchronously
            processAIResponse(projectId, content, threadId, llmProviderId, principal);
            
        } catch (Exception e) {
            log.error("Error handling chat message", e);
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/chat/" + projectId,
                Map.of(
                    "type", "error",
                    "message", "Failed to process message: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                )
            );
        }
    }

    @MessageMapping("/action-plan/{planId}/subscribe")
    public void subscribeToActionPlanUpdates(
            @DestinationVariable Long planId,
            Principal principal) {
        
        log.info("User {} subscribed to action plan updates for plan {}", principal.getName(), planId);
        
        // Send initial status
        try {
            var actionPlan = actionPlanService.getActionPlan(planId, null); // Token validation would be needed here
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/action-plan/" + planId,
                Map.of(
                    "type", "status_update",
                    "plan", actionPlan,
                    "timestamp", System.currentTimeMillis()
                )
            );
        } catch (Exception e) {
            log.error("Error sending initial action plan status", e);
        }
    }

    private void processAIResponse(Long projectId, String content, String threadId, 
                                   Long llmProviderId, Principal principal) {
        
        // This would typically be executed in a separate thread or async service
        try {
            // Simulate AI processing
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/chat/" + projectId,
                Map.of(
                    "type", "ai_thinking",
                    "message", "AI is processing your request...",
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // In a real implementation, you would call the AI service here
            // For now, we'll simulate a response
            Thread.sleep(1000);
            
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/chat/" + projectId,
                Map.of(
                    "type", "ai_response",
                    "content", "This is a simulated AI response to: " + content,
                    "threadId", threadId,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
        } catch (Exception e) {
            log.error("Error processing AI response", e);
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/chat/" + projectId,
                Map.of(
                    "type", "error",
                    "message", "AI processing failed: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                )
            );
        }
    }

    // Method to broadcast action plan updates to all subscribers
    public void broadcastActionPlanUpdate(Long planId, Object update) {
        messagingTemplate.convertAndSend("/topic/action-plan/" + planId, update);
    }

    // Method to send project-specific updates
    public void sendProjectUpdate(Long projectId, Object update) {
        messagingTemplate.convertAndSend("/topic/project/" + projectId, update);
    }

    // Method to send user-specific notifications
    public void sendUserNotification(String username, Object notification) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
    }
}
