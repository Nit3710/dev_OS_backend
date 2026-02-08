package com.devos.api.dto;

import com.devos.core.domain.entity.PlanStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private Map<String, Object> parameters;
    private List<Integer> dependencies;
    private Integer estimatedDurationSeconds;
    private Integer actualDurationSeconds;
    private Long actionPlanId;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private Boolean isCritical;
    private Double confidenceScore;
    
    public static PlanStepDto from(PlanStep planStep) {
        return PlanStepDto.builder()
                .id(planStep.getId())
                .stepNumber(planStep.getStepNumber())
                .title(planStep.getTitle())
                .description(planStep.getDescription())
                .type(planStep.getType())
                .status(planStep.getStatus())
                .parameters(planStep.getParameters())
                .dependencies(planStep.getDependencies())
                .estimatedDurationSeconds(planStep.getEstimatedDurationSeconds())
                .actualDurationSeconds(planStep.getActualDurationSeconds())
                .actionPlanId(planStep.getActionPlan() != null ? planStep.getActionPlan().getId() : null)
                .createdAt(planStep.getCreatedAt())
                .startedAt(planStep.getStartedAt())
                .completedAt(planStep.getCompletedAt())
                .errorMessage(planStep.getErrorMessage())
                .retryCount(planStep.getRetryCount())
                .maxRetries(planStep.getMaxRetries())
                .isCritical(planStep.getIsCritical())
                .confidenceScore(planStep.getConfidenceScore())
                .build();
    }
}
