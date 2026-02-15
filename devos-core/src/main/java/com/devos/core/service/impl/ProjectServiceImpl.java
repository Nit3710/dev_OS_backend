package com.devos.core.service.impl;

import com.devos.core.domain.entity.Project;
import com.devos.core.domain.entity.User;
import com.devos.core.repository.ProjectRepository;
import com.devos.core.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
    private final @Lazy com.devos.core.service.AuthService authService;
    private final com.devos.core.service.GitService gitService;

    @Override
    public Object getFileTree(Long id, boolean includeContent) {
        Project project = getProjectWithOwnership(id);
        String localPath = project.getLocalPath();
        
        if (localPath == null || localPath.isEmpty()) {
             return java.util.Collections.emptyList();
        }

        java.io.File projectDir = new java.io.File(localPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            return java.util.Collections.emptyList();
        }

        return buildFileTree(projectDir, projectDir.getAbsolutePath());
    }

    private java.util.List<java.util.Map<String, Object>> buildFileTree(java.io.File dir, String rootPath) {
        java.io.File[] files = dir.listFiles();
        if (files == null) return java.util.Collections.emptyList();

        java.util.List<java.util.Map<String, Object>> tree = new java.util.ArrayList<>();
        
        // sort: directories first, then files
        java.util.Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        for (java.io.File file : files) {
            // Skip hidden files and common ignore patterns
            if (file.getName().startsWith(".") && !file.getName().equals(".gitignore")) continue;
            if (file.getName().equals("node_modules") || file.getName().equals("target") || file.getName().equals("dist") || file.getName().equals("build")) continue;

            java.util.Map<String, Object> node = new java.util.HashMap<>();
            // Use a relative path as ID to ensure uniqueness but stability
            String relativePath = file.getAbsolutePath().substring(rootPath.length()).replace("\\", "/"); 
            if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
            
            node.put("id", relativePath.isEmpty() ? file.getName() : relativePath);
            node.put("name", file.getName());
            node.put("type", file.isDirectory() ? "directory" : "file");
            // node.put("path", relativePath); 

            if (file.isDirectory()) {
                node.put("children", buildFileTree(file, rootPath));
            }

            tree.add(node);
        }
        return tree;
    }

    @Override
    @Transactional
    public Project createProject(Project project) {
        com.devos.core.domain.entity.User currentUser = authService.getCurrentUser();
        project.setUser(currentUser);
        project.setCreatedAt(java.time.LocalDateTime.now());
        project.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Set default values to prevent null constraint violations
        if (project.getStatus() == null) {
            project.setStatus(com.devos.core.domain.entity.Project.ProjectStatus.ACTIVE);
        }
        if (project.getIsIndexed() == null) {
            project.setIsIndexed(false);
        }
        if (project.getGitBranch() == null) {
            project.setGitBranch("main");
        }
        
        // Handle project type logic
        boolean shouldClone = false;
        if (project.getRepositoryUrl() != null && !project.getRepositoryUrl().isEmpty()) {
            // Remote repository project - localPath can be null or set to a default
            if (project.getLocalPath() == null || project.getLocalPath().isEmpty()) {
                project.setLocalPath(generateDefaultLocalPath(project.getRepositoryUrl()));
            }
            shouldClone = true;
        } else if (project.getLocalPath() == null || project.getLocalPath().isEmpty()) {
            throw new IllegalArgumentException("Either localPath or repositoryUrl must be provided");
        }
        
        Project savedProject = projectRepository.save(project);
        
        if (shouldClone) {
            try {
                gitService.cloneRepository(savedProject.getLocalPath(), savedProject.getRepositoryUrl());
                // Auto-detect language and file count after cloning
                ProjectMetadata metadata = detectProjectMetadata(savedProject.getLocalPath());
                savedProject.setLanguage(metadata.language);
                savedProject.setFileCount(metadata.fileCount);
                projectRepository.save(savedProject);
            } catch (Exception e) {
                log.error("Failed to clone repository: {}", e.getMessage());
                throw new RuntimeException("Failed to clone repository during project creation", e);
            }
        } else {
            // If local path provided, detect metadata immediately
            ProjectMetadata metadata = detectProjectMetadata(savedProject.getLocalPath());
            savedProject.setLanguage(metadata.language);
            savedProject.setFileCount(metadata.fileCount);
            projectRepository.save(savedProject);
        }
        
        log.info("Created project: {} for user: {}", savedProject.getName(), currentUser.getUsername());
        
        return savedProject;
    }

    private static class ProjectMetadata {
        String language;
        int fileCount;
    }

    private ProjectMetadata detectProjectMetadata(String localPath) {
        ProjectMetadata metadata = new ProjectMetadata();
        metadata.language = "Unknown";
        metadata.fileCount = 0;

        if (localPath == null || localPath.isEmpty()) return metadata;
        
        java.io.File projectDir = new java.io.File(localPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) return metadata;

        java.util.Map<String, Integer> extensionCounts = new java.util.HashMap<>();
        int[] totalFiles = new int[1]; // Use array to allow modification in lambda/recursion
        scanExtensions(projectDir, extensionCounts, totalFiles);

        metadata.fileCount = totalFiles[0];

        if (extensionCounts.isEmpty()) return metadata;

        // Map extensions to human-readable names
        java.util.Map<String, String> extensionToLanguage = new java.util.HashMap<>();
        extensionToLanguage.put("java", "Java");
        extensionToLanguage.put("py", "Python");
        extensionToLanguage.put("js", "JavaScript");
        extensionToLanguage.put("ts", "TypeScript");
        extensionToLanguage.put("tsx", "TypeScript");
        extensionToLanguage.put("jsx", "JavaScript");
        extensionToLanguage.put("cpp", "C++");
        extensionToLanguage.put("c", "C");
        extensionToLanguage.put("go", "Go");
        extensionToLanguage.put("rs", "Rust");
        extensionToLanguage.put("php", "PHP");
        extensionToLanguage.put("rb", "Ruby");
        extensionToLanguage.put("kt", "Kotlin");
        extensionToLanguage.put("swift", "Swift");
        extensionToLanguage.put("cs", "C#");

        metadata.language = extensionCounts.entrySet().stream()
                .filter(e -> extensionToLanguage.containsKey(e.getKey()))
                .max(java.util.Map.Entry.comparingByValue())
                .map(e -> extensionToLanguage.get(e.getKey()))
                .orElse("Unknown");

        return metadata;
    }

    private void scanExtensions(java.io.File dir, java.util.Map<String, Integer> counts, int[] totalFiles) {
        java.io.File[] files = dir.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                // Skip common large folders
                if (file.getName().equals("node_modules") || file.getName().equals("target") || 
                    file.getName().equals(".git") || file.getName().equals("venv") ||
                    file.getName().equals("dist") || file.getName().equals("build")) continue;
                scanExtensions(file, counts, totalFiles);
            } else {
                totalFiles[0]++;
                String name = file.getName();
                int lastDot = name.lastIndexOf('.');
                if (lastDot > 0 && lastDot < name.length() - 1) {
                    String ext = name.substring(lastDot + 1).toLowerCase();
                    counts.put(ext, counts.getOrDefault(ext, 0) + 1);
                }
            }
        }
    }
    
    private String generateDefaultLocalPath(String repositoryUrl) {
        // Extract repo name from URL and create a default local path
        String repoName = repositoryUrl.substring(repositoryUrl.lastIndexOf('/') + 1);
        if (repoName.endsWith(".git")) {
            repoName = repoName.substring(0, repoName.length() - 4);
        }
        return "./projects/" + repoName;
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
        User currentUser = authService.getCurrentUser();
        return projectRepository.findByUserId(currentUser.getId(), pageable);
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
