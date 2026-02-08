package com.devos.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "action_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ActionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.DRAFT;

    @Column(name = "total_steps")
    private Integer totalSteps = 0;

    @Column(name = "completed_steps")
    private Integer completedSteps = 0;

    @Column(name = "failed_steps")
    private Integer failedSteps = 0;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(name = "rollback_commit_hash")
    private String rollbackCommitHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "actionPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<PlanStep> planSteps = new HashSet<>();

    @OneToMany(mappedBy = "actionPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FileChange> fileChanges = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "rollback_at")
    private LocalDateTime rollbackAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "risk_level")
    private String riskLevel;

    public enum PlanStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK, CANCELLED,PAUSED
    }
}
