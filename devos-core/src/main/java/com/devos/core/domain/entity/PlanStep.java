package com.devos.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "plan_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer stepNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status = StepStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "json")
    private Map<String, Object> parameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dependencies", columnDefinition = "json")
    private List<Integer> dependencies;

    @Column(name = "estimated_duration_seconds")
    private Integer estimatedDurationSeconds;

    @Column(name = "actual_duration_seconds")
    private Integer actualDurationSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_plan_id", nullable = false)
    private ActionPlan actionPlan;

    @OneToMany(mappedBy = "planStep", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileChange> fileChanges;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "is_critical")
    private Boolean isCritical = false;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    public enum StepType {
        CREATE_FILE, UPDATE_FILE, DELETE_FILE, RUN_COMMAND, GIT_OPERATION, AI_ANALYSIS, CUSTOM
    }

    public enum StepStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED, CANCELLED
    }
}
