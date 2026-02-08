package com.devos.core.repository;

import com.devos.core.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    List<AuditLog> findByProjectIdAndActionOrderByCreatedAtDesc(Long projectId, AuditLog.AuditAction action);

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findByActionOrderByCreatedAtDesc(AuditLog.AuditAction action);

    @Query("SELECT a FROM AuditLog a WHERE a.project.id = :projectId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentLogs(@Param("projectId") Long projectId, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :since AND a.success = false ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentErrors(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.project.id = :projectId AND a.action = :action AND a.createdAt >= :since")
    long countByProjectAndActionSince(@Param("projectId") Long projectId, @Param("action") AuditLog.AuditAction action, @Param("since") LocalDateTime since);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.project.id = :projectId AND a.createdAt >= :since GROUP BY a.action")
    List<Object[]> getActionStatistics(@Param("projectId") Long projectId, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM AuditLog a WHERE a.description LIKE %:search% AND a.project.id = :projectId")
    List<AuditLog> searchLogs(@Param("projectId") Long projectId, @Param("search") String search);
}
