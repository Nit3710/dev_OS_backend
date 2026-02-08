package com.devos.api.dto;

import com.devos.core.domain.entity.ActionPlan;
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
public class ActionPlanDto {
    
    private Long id;
    private String title;
    private String description;
    private ActionPlan.PlanStatus status;
    private Integer totalSteps;
    private Integer completedSteps;
    private Integer failedSteps;
    private Integer estimatedDurationMinutes;
    private Integer actualDurationMinutes;
    private String rollbackCommitHash;
    private Long projectId;
    private List<PlanStepDto> planSteps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime executedAt;
    private LocalDateTime completedAt;
    private LocalDateTime rollbackAt;
    private String createdBy;
    private Double confidenceScore;
    private String riskLevel;
    
    public static ActionPlanDto from(ActionPlan actionPlan) {
        return ActionPlanDto.builder()
                .id(actionPlan.getId())
                .title(actionPlan.getTitle())
                .description(actionPlan.getDescription())
                .status(actionPlan.getStatus())
                .totalSteps(actionPlan.getTotalSteps())
                .completedSteps(actionPlan.getCompletedSteps())
                .failedSteps(actionPlan.getFailedSteps())
                .estimatedDurationMinutes(actionPlan.getEstimatedDurationMinutes())
                .actualDurationMinutes(actionPlan.getActualDurationMinutes())
                .rollbackCommitHash(actionPlan.getRollbackCommitHash())
                .projectId(actionPlan.getProject() != null ? actionPlan.getProject().getId() : null)
                .planSteps(actionPlan.getPlanSteps() != null ? 
                    actionPlan.getPlanSteps().stream()
                        .map(PlanStepDto::from)
                        .toList() : null)
                .createdAt(actionPlan.getCreatedAt())
                .updatedAt(actionPlan.getUpdatedAt())
                .executedAt(actionPlan.getExecutedAt())
                .completedAt(actionPlan.getCompletedAt())
                .rollbackAt(actionPlan.getRollbackAt())
                .createdBy(actionPlan.getCreatedBy())
                .confidenceScore(actionPlan.getConfidenceScore())
                .riskLevel(actionPlan.getRiskLevel())
                .build();
    }
}
