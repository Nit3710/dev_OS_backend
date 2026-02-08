package com.devos.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ChatRequest {
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    @NotNull(message = "Project ID is required")
    private Long projectId;
    
    private String threadId;
    private Long llmProviderId;
    private Map<String, Object> context;
    private Boolean stream = false;
    private Integer maxTokens;
    private Double temperature;
    private Boolean includeActionPlan = false;
}
