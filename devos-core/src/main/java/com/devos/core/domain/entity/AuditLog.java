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
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "json")
    private Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "json")
    private Map<String, Object> newValues;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "success")
    private Boolean success = true;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "correlation_id")
    private String correlationId;

    public enum AuditAction {
        CREATE, UPDATE, DELETE, READ, LOGIN, LOGOUT, APPROVE, REJECT, EXECUTE, ROLLBACK, INDEX, SEARCH
    }
}
