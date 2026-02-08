package com.devos.core.service.impl;

import com.devos.core.domain.entity.AuditLog;
import com.devos.core.repository.AuditLogRepository;
import com.devos.core.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void logActivity(String action, String entity, Long entityId, Long userId, Map<String, Object> details) {
        AuditLog auditLog = AuditLog.builder()
                .action(AuditLog.AuditAction.valueOf(action))
                .entityType(entity)
                .entityId(entityId)
                .userId(userId)
                .oldValues(details != null && details.containsKey("oldValues") ? (Map<String, Object>) details.get("oldValues") : null)
                .newValues(details != null && details.containsKey("newValues") ? (Map<String, Object>) details.get("newValues") : null)
                .description(details != null && details.containsKey("description") ? details.get("description").toString() : null)
                .createdAt(LocalDateTime.now())
                .build();
        
        auditLogRepository.save(auditLog);
        log.info("Logged activity: {} for entity: {} by user: {}", action, entity, userId);
    }

    @Override
    public Page<AuditLog> getAuditLogs(Long projectId, AuditLog.AuditAction action, LocalDateTime since, LocalDateTime until, Pageable pageable, String token) {
        if (action != null && since != null && until != null) {
            List<AuditLog> filteredLogs = auditLogRepository.findByProjectIdAndActionOrderByCreatedAtDesc(projectId, action)
                    .stream()
                    .filter(log -> log.getCreatedAt().isAfter(since) && log.getCreatedAt().isBefore(until))
                    .toList();
            return new PageImpl<>(filteredLogs, pageable, filteredLogs.size());
        } else if (action != null) {
            List<AuditLog> filteredLogs = auditLogRepository.findByProjectIdAndActionOrderByCreatedAtDesc(projectId, action)
                    .stream()
                    .filter(log -> since == null || log.getCreatedAt().isAfter(since))
                    .filter(log -> until == null || log.getCreatedAt().isBefore(until))
                    .toList();
            return new PageImpl<>(filteredLogs, pageable, filteredLogs.size());
        } else {
            return auditLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
        }
    }

    @Override
    public List<Map<String, Object>> getSystemActivity(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<AuditLog> logs = auditLogRepository.findByActionOrderByCreatedAtDesc(AuditLog.AuditAction.LOGIN)
                .stream()
                .filter(log -> log.getCreatedAt().isAfter(since))
                .toList();
        
        Map<String, Object> systemActivity = new HashMap<>();
        systemActivity.put("totalLogs", logs.size());
        systemActivity.put("timeRange", hours + " hours");
        systemActivity.put("logs", logs);
        
        return List.of(systemActivity);
    }

    @Override
    public Page<AuditLog> getProjectActivity(Long projectId, Pageable pageable, String token) {
        return auditLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
    }

    @Override
    public Map<String, Object> getProjectStats(Long projectId, int days, String token) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> stats = auditLogRepository.getActionStatistics(projectId, since);
        
        Map<String, Object> projectStats = new HashMap<>();
        projectStats.put("projectId", projectId);
        projectStats.put("timeRange", days + " days");
        projectStats.put("totalLogs", stats.size());
        
        // Convert Object[] to Map
        Map<String, Long> actionCounts = new HashMap<>();
        stats.forEach(stat -> actionCounts.put((String) stat[0], (Long) stat[1]));
        projectStats.put("actionCounts", actionCounts);
        
        return projectStats;
    }

    @Override
    public List<AuditLog> getRecentErrors(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return auditLogRepository.findRecentErrors(since);
    }

    @Override
    public Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable) {
        List<AuditLog> logs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return new PageImpl<>(logs, pageable, logs.size());
    }

    @Override
    public List<AuditLog> getUserActivity(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public AuditLog createAuditLog(Map<String, Object> logData, String token) {
        AuditLog auditLog = AuditLog.builder()
                .action(AuditLog.AuditAction.valueOf(logData.get("action").toString()))
                .entityType(logData.get("entityType").toString())
                .entityId(logData.get("entityId") != null ? Long.valueOf(logData.get("entityId").toString()) : null)
                .userId(logData.get("userId") != null ? Long.valueOf(logData.get("userId").toString()) : null)
                .oldValues(logData.containsKey("oldValues") ? (Map<String, Object>) logData.get("oldValues") : null)
                .newValues(logData.containsKey("newValues") ? (Map<String, Object>) logData.get("newValues") : null)
                .description(logData.containsKey("description") ? logData.get("description").toString() : null)
                .createdAt(LocalDateTime.now())
                .build();
        
        AuditLog savedLog = auditLogRepository.save(auditLog);
        log.info("Created audit log: {}", savedLog.getId());
        
        return savedLog;
    }

    @Override
    public byte[] exportAuditLogs(Long projectId, LocalDateTime since, LocalDateTime until, String format, String token) {
        List<AuditLog> logs = auditLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, Pageable.unpaged())
                .stream()
                .filter(log -> since == null || log.getCreatedAt().isAfter(since))
                .filter(log -> until == null || log.getCreatedAt().isBefore(until))
                .toList();
        
        // Simple CSV export implementation
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Action,Entity Type,Entity ID,User ID,Created At,Description,Success\n");
        
        for (AuditLog log : logs) {
            csv.append(log.getId()).append(",")
               .append(log.getAction()).append(",")
               .append(log.getEntityType()).append(",")
               .append(log.getEntityId()).append(",")
               .append(log.getUserId()).append(",")
               .append(log.getCreatedAt()).append(",")
               .append("\"").append(log.getDescription() != null ? log.getDescription().replace("\"", "\"\"") : "").append("\",")
               .append(log.getSuccess()).append("\n");
        }
        
        log.info("Exported {} audit logs for project {}", logs.size(), projectId);
        return csv.toString().getBytes();
    }
}
