package com.devos.core.dto;

import com.devos.core.domain.entity.PlanStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanStepDto {
    
    private Long id;
    
    private Integer stepNumber;
    
    private String title;
    
    private String description;
    
    private PlanStep.StepType type;
    
    private PlanStep.StepStatus status;
    
    private String parameters;
    
    private List<Integer> dependencies;
    
    private Integer estimatedDurationSeconds;
    
    private Integer actualDurationSeconds;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    private String errorMessage;
    
    private Integer retryCount;
    
    private Integer maxRetries;
    
    private Boolean isCritical;
    
    private Double confidenceScore;
    
    private Long actionPlanId;
}
