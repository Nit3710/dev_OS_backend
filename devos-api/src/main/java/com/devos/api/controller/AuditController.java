package com.devos.api.controller;

import com.devos.core.domain.entity.AuditLog;
import com.devos.core.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs/{projectId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestHeader("Authorization") String token,
            @PathVariable("projectId") Long projectId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(name = "action", required = false) AuditLog.AuditAction action,
            @RequestParam(name = "since", required = false) LocalDateTime since,
            @RequestParam(name = "until", required = false) LocalDateTime until) {
        
        String jwtToken = token.replace("Bearer ", "");
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AuditLog> logs = auditService.getAuditLogs(
            projectId, action, since, until, pageable, jwtToken);
        
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getSystemActivity(
            @RequestParam(name = "hours", defaultValue = "24") int hours) {
        
        List<Map<String, Object>> activity = auditService.getSystemActivity(hours);
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/stats/{projectId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getProjectStats(
            @RequestHeader("Authorization") String token,
            @PathVariable("projectId") Long projectId,
            @RequestParam(name = "days", defaultValue = "7") int days) {
        
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> stats = auditService.getProjectStats(projectId, days, jwtToken);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/errors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getRecentErrors(
            @RequestParam(name = "hours", defaultValue = "24") int hours) {
        
        List<AuditLog> errors = auditService.getRecentErrors(hours);
        return ResponseEntity.ok(errors);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @auditService.isCurrentUser(#userId, authentication)")
    public ResponseEntity<Page<AuditLog>> getUserAuditLogs(
            @PathVariable("userId") Long userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> logs = auditService.getUserAuditLogs(userId, pageable);
        
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/log")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<AuditLog> createAuditLog(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> logData) {
        
        String jwtToken = token.replace("Bearer ", "");
        AuditLog auditLog = auditService.createAuditLog(logData, jwtToken);
        
        return ResponseEntity.ok(auditLog);
    }

    @GetMapping("/export/{projectId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestHeader("Authorization") String token,
            @PathVariable("projectId") Long projectId,
            @RequestParam(name = "since", required = false) LocalDateTime since,
            @RequestParam(name = "until", required = false) LocalDateTime until,
            @RequestParam(name = "format", defaultValue = "json") String format) {
        
        String jwtToken = token.replace("Bearer ", "");
        byte[] exportData = auditService.exportAuditLogs(projectId, since, until, format, jwtToken);
        
        String filename = String.format("audit-logs-%d.%s", projectId, format);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(exportData);
    }
}
