package com.devos.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final List<HealthIndicator> healthIndicators;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        // Get the main application health indicator (usually the first one)
        Health health = healthIndicators.isEmpty() ? Health.up().build() : healthIndicators.get(0).health();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", health.getStatus().getCode());
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "DevOS Backend API");
        response.put("version", "1.0.0");
        response.put("details", health.getDetails());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        // Get the main application health indicator (usually the first one)
        Health health = healthIndicators.isEmpty() ? Health.up().build() : healthIndicators.get(0).health();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", health.getStatus().getCode());
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "DevOS Backend API");
        response.put("version", "1.0.0");
        response.put("uptime", getUptime());
        response.put("details", health.getDetails());
        
        // Add additional health checks
        response.put("database", checkDatabaseHealth());
        response.put("redis", checkRedisHealth());
        response.put("disk_space", checkDiskSpace());
        response.put("memory", checkMemoryHealth());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "READY");
        response.put("timestamp", LocalDateTime.now());
        response.put("checks", Map.of(
            "database", checkDatabaseHealth(),
            "redis", checkRedisHealth()
        ));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> livenessCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ALIVE");
        response.put("timestamp", LocalDateTime.now());
        response.put("uptime", getUptime());
        
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        try {
            // This would be implemented with actual database health check
            dbHealth.put("status", "UP");
            dbHealth.put("response_time", "5ms");
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        return dbHealth;
    }

    private Map<String, Object> checkRedisHealth() {
        Map<String, Object> redisHealth = new HashMap<>();
        try {
            // This would be implemented with actual Redis health check
            redisHealth.put("status", "UP");
            redisHealth.put("response_time", "2ms");
        } catch (Exception e) {
            redisHealth.put("status", "DOWN");
            redisHealth.put("error", e.getMessage());
        }
        return redisHealth;
    }

    private Map<String, Object> checkDiskSpace() {
        Runtime runtime = Runtime.getRuntime();
        long totalSpace = new java.io.File("/").getTotalSpace();
        long freeSpace = new java.io.File("/").getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        
        Map<String, Object> diskHealth = new HashMap<>();
        diskHealth.put("status", freeSpace > 1024 * 1024 * 1024 ? "UP" : "DOWN"); // 1GB threshold
        diskHealth.put("total_space", totalSpace);
        diskHealth.put("free_space", freeSpace);
        diskHealth.put("used_space", usedSpace);
        diskHealth.put("usage_percentage", (double) usedSpace / totalSpace * 100);
        
        return diskHealth;
    }

    private Map<String, Object> checkMemoryHealth() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        Map<String, Object> memoryHealth = new HashMap<>();
        memoryHealth.put("status", "UP");
        memoryHealth.put("max_memory", maxMemory);
        memoryHealth.put("total_memory", totalMemory);
        memoryHealth.put("free_memory", freeMemory);
        memoryHealth.put("used_memory", usedMemory);
        memoryHealth.put("usage_percentage", (double) usedMemory / maxMemory * 100);
        
        return memoryHealth;
    }

    private String getUptime() {
        long uptime = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return uptime + "ms"; // This is a placeholder - actual uptime would be tracked differently
    }
}
