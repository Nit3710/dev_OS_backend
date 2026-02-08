package com.devos.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "file_operations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType type;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "source_path")
    private String sourcePath;

    @Column(name = "destination_path")
    private String destinationPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationStatus status = OperationStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "checksum_before")
    private String checksumBefore;

    @Column(name = "checksum_after")
    private String checksumAfter;

    @Column(name = "backup_path")
    private String backupPath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "is_rollback_available")
    private Boolean isRollbackAvailable = false;

    @Column(name = "rollback_data")
    private String rollbackData;

    public enum OperationType {
        READ, WRITE, CREATE, DELETE, MOVE, COPY, BACKUP, RESTORE, INDEX, SEARCH,UPDATE
    }

    public enum OperationStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }
}
