package com.devos.api.dto;

import com.devos.core.domain.entity.AIMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIMessageDto {
    
    private Long id;
    private AIMessage.MessageType type;
    private String content;
    private Map<String, Object> metadata;
    private Integer tokenCount;
    private Long processingTimeMs;
    private Long projectId;
    private Long llmProviderId;
    private Long actionPlanId;
    private String threadId;
    private Long parentMessageId;
    private LocalDateTime createdAt;
    private String modelUsed;
    private Double cost;
    private Boolean isStreaming;
    private String streamId;
    
    public static AIMessageDto from(AIMessage message) {
        return AIMessageDto.builder()
                .id(message.getId())
                .type(message.getType())
                .content(message.getContent())
                .metadata(message.getMetadata())
                .tokenCount(message.getTokenCount())
                .processingTimeMs(message.getProcessingTimeMs())
                .projectId(message.getProject() != null ? message.getProject().getId() : null)
                .llmProviderId(message.getLlmProvider() != null ? message.getLlmProvider().getId() : null)
                .actionPlanId(message.getActionPlan() != null ? message.getActionPlan().getId() : null)
                .threadId(message.getThreadId())
                .parentMessageId(message.getParentMessageId())
                .createdAt(message.getCreatedAt())
                .modelUsed(message.getModelUsed())
                .cost(message.getCost())
                .isStreaming(message.getIsStreaming())
                .streamId(message.getStreamId())
                .build();
    }
}
