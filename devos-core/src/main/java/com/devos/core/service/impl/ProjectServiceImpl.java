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
    private final com.devos.core.service.AuthService authService;

    @Override
    public Object getFileTree(Long id, boolean includeContent) {
        Project project = getProjectWithOwnership(id);
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
    public Project createProject(Project project) {
        com.devos.core.domain.entity.User currentUser = authService.getCurrentUser();
        project.setUser(currentUser);
        project.setCreatedAt(java.time.LocalDateTime.now());
        project.setUpdatedAt(java.time.LocalDateTime.now());
        
        Project savedProject = projectRepository.save(project);
        log.info("Created project: {} for user: {}", savedProject.getName(), currentUser.getUsername());
        
        return savedProject;
    }

    @Override
    public Project getProject(Long id) {
        return getProjectWithOwnership(id);
    }

    @Override
    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    @Override
    public Page<Project> getUserProjects(Pageable pageable) {
        com.devos.core.domain.entity.User currentUser = authService.getCurrentUser();
        List<Project> projects = projectRepository.findByUserId(currentUser.getId());
        return new org.springframework.data.domain.PageImpl<>(projects, pageable, projects.size());
    }

    @Override
    @Transactional
    public Project updateProject(Long id, Project project) {
        Project existingProject = getProjectWithOwnership(id);
        
        existingProject.setName(project.getName());
        existingProject.setDescription(project.getDescription());
        existingProject.setUpdatedAt(java.time.LocalDateTime.now());
        
        Project updatedProject = projectRepository.save(existingProject);
        log.info("Updated project: {}", updatedProject.getName());
        
        return updatedProject;
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        Project project = getProjectWithOwnership(id);
        projectRepository.delete(project);
        log.info("Deleted project: {}", project.getName());
    }

    @Override
    public List<Project> searchProjects(String query) {
        com.devos.core.domain.entity.User currentUser = authService.getCurrentUser();
        return projectRepository.searchProjects(query, currentUser.getId());
    }

    private Project getProjectWithOwnership(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        
        com.devos.core.domain.entity.User currentUser = authService.getCurrentUser();
        if (!project.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Access denied: You do not own this project");
        }
        return project;
    }
}
