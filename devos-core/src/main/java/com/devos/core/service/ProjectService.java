package com.devos.core.service;

import com.devos.core.domain.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProjectService {
    
    Object getFileTree(Long id, boolean includeContent);
    
    Project createProject(Project project);
    
    Project getProject(Long id);
    
    Optional<Project> getProjectById(Long id);
    
    Page<Project> getUserProjects(Pageable pageable);
    
    Project updateProject(Long id, Project project);
    
    void deleteProject(Long id);
    
    List<Project> searchProjects(String query);
}
