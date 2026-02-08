package com.devos.core.service.impl;

import com.devos.core.domain.entity.Project;
import com.devos.core.repository.ProjectRepository;
import com.devos.core.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    public Object getFileTree(Long id, boolean includeContent, String token) {
        Project project = getProjectById(id).orElseThrow(() -> 
            new RuntimeException("Project not found with id: " + id));
        // This would typically return a file tree structure
        // For now, return a simple response
        return java.util.Map.of(
                "projectId", id,
                "projectName", project.getName(),
                "includeContent", includeContent
        );
    }

    @Override
    @Transactional
    public Project createProject(Project project, String token) {
        project.setCreatedAt(java.time.LocalDateTime.now());
        project.setUpdatedAt(java.time.LocalDateTime.now());
        
        Project savedProject = projectRepository.save(project);
        log.info("Created project: {}", savedProject.getName());
        
        return savedProject;
    }

    @Override
    public Project getProject(Long id, String token) {
        return getProjectById(id).orElseThrow(() -> 
            new RuntimeException("Project not found with id: " + id));
    }

    @Override
    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    @Override
    public Page<Project> getUserProjects(Long userId, Pageable pageable) {
        List<Project> projects = projectRepository.findByUserId(userId);
        return new org.springframework.data.domain.PageImpl<>(projects, pageable, projects.size());
    }

    @Override
    @Transactional
    public Project updateProject(Long id, Project project, String token) {
        Project existingProject = getProjectById(id).orElseThrow(() -> 
            new RuntimeException("Project not found with id: " + id));
        
        existingProject.setName(project.getName());
        existingProject.setDescription(project.getDescription());
        existingProject.setUpdatedAt(java.time.LocalDateTime.now());
        
        Project updatedProject = projectRepository.save(existingProject);
        log.info("Updated project: {}", updatedProject.getName());
        
        return updatedProject;
    }

    @Override
    @Transactional
    public void deleteProject(Long id, String token) {
        Project project = getProjectById(id).orElseThrow(() -> 
            new RuntimeException("Project not found with id: " + id));
        projectRepository.delete(project);
        log.info("Deleted project: {}", project.getName());
    }

    @Override
    public Long getUserIdFromToken(String token) {
        // This would typically decode JWT token and extract user ID
        // For now, return a placeholder implementation
        return 1L; // Placeholder
    }

    @Override
    public List<Project> searchProjects(String query, Long userId) {
        return projectRepository.searchProjects(query, userId);
    }
}
