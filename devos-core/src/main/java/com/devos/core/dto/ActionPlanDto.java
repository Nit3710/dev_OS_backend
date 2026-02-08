package com.devos.core.dto;

import com.devos.core.domain.entity.ActionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionPlanDto {
    
    private Long id;
    
    private String title;
    
    private String description;
    
    private ActionPlan.PlanStatus status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime executedAt;
    
    private LocalDateTime completedAt;
    
    private LocalDateTime rollbackAt;
    
    private Long projectId;
    
    private Integer totalSteps;
    
    private Integer completedSteps;
    
    private Integer failedSteps;
    
    private Integer estimatedDurationMinutes;
    
    private Integer actualDurationMinutes;
    
    private String rollbackCommitHash;
    
    private String createdBy;
    
    private Double confidenceScore;
    
    private String riskLevel;
}
