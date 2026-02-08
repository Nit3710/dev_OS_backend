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

@Entity
@Table(name = "file_changes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType type;

    @Column(name = "old_content", columnDefinition = "TEXT")
    private String oldContent;

    @Column(name = "new_content", columnDefinition = "TEXT")
    private String newContent;

    @Column(name = "backup_path")
    private String backupPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_plan_id")
    private ActionPlan actionPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_step_id")
    private PlanStep planStep;

    @OneToMany(mappedBy = "fileChange", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiffChunk> diffChunks;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "rollback_at")
    private LocalDateTime rollbackAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeStatus status = ChangeStatus.PENDING;

    @Column(name = "line_changes")
    private Integer lineChanges;

    @Column(name = "character_changes")
    private Integer characterChanges;

    @Column(name = "checksum_before")
    private String checksumBefore;

    @Column(name = "checksum_after")
    private String checksumAfter;

    @Column(name = "is_conflict")
    private Boolean isConflict = false;

    @Column(name = "conflict_resolution")
    private String conflictResolution;

    public enum ChangeType {
        CREATE, UPDATE, DELETE, MOVE, COPY
    }

    public enum ChangeStatus {
        PENDING, APPLIED, FAILED, ROLLED_BACK, CONFLICT
    }
}
