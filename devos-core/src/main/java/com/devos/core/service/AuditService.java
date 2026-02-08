package com.devos.core.service;

import com.devos.core.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AuditService {
    
    void logActivity(String action, String entity, Long entityId, Long userId, Map<String, Object> details);
    
    Page<AuditLog> getAuditLogs(Long projectId, AuditLog.AuditAction action, LocalDateTime since, LocalDateTime until, Pageable pageable, String token);
    
    List<Map<String, Object>> getSystemActivity(int hours);
    
    Page<AuditLog> getProjectActivity(Long projectId, Pageable pageable, String token);
    
    Map<String, Object> getProjectStats(Long projectId, int days, String token);
    
    List<AuditLog> getRecentErrors(int hours);
    
    Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable);
    
    List<AuditLog> getUserActivity(Long userId, Pageable pageable);
    
    AuditLog createAuditLog(Map<String, Object> logData, String token);
    
    byte[] exportAuditLogs(Long projectId, LocalDateTime since, LocalDateTime until, String format, String token);
}
