package com.devos.core.service;

import com.devos.core.domain.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProjectService {
    
    Object getFileTree(Long id, boolean includeContent, String token);
    
    Project createProject(Project project, String token);
    
    Project getProject(Long id, String token);
    
    Optional<Project> getProjectById(Long id);
    
    Page<Project> getUserProjects(Long userId, Pageable pageable);
    
    Project updateProject(Long id, Project project, String token);
    
    void deleteProject(Long id, String token);
    
    Long getUserIdFromToken(String token);
    
    List<Project> searchProjects(String query, Long userId);
}
