package com.devos.api.controller;

import com.devos.api.dto.ProjectDto;
import com.devos.core.domain.entity.Project;
import com.devos.core.service.ProjectService;
import com.devos.core.service.FileIndexingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final FileIndexingService fileIndexingService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProjectDto>> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Project> projects = projectService.getUserProjects(pageable);
        Page<ProjectDto> projectDtos = projects.map(ProjectDto::from);
        
        return ResponseEntity.ok(projectDtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDto> getProject(@PathVariable Long id) {
        Project project = projectService.getProject(id);
        return ResponseEntity.ok(ProjectDto.from(project));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDto> createProject(@Valid @RequestBody ProjectDto projectDto) {
        Project project = projectService.createProject(projectDto.toEntity());
        
        log.info("Project created: {} for user ID: {}", project.getName(), project.getUser().getId());
        return ResponseEntity.ok(ProjectDto.from(project));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectDto projectDto) {
        
        Project project = projectService.updateProject(id, projectDto.toEntity());
        
        log.info("Project updated: {}", project.getName());
        return ResponseEntity.ok(ProjectDto.from(project));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        
        log.info("Project deleted with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/file-tree")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> getFileTree(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeContent) {
        
        Object fileTree = projectService.getFileTree(id, includeContent);
        
        return ResponseEntity.ok(fileTree);
    }

    @PostMapping("/{id}/index")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> indexProject(@PathVariable Long id) {
        // fileIndexingService should also be refactored or handles internally
        fileIndexingService.indexProject(id);
        
        log.info("Project indexing started for ID: {}", id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectDto>> searchProjects(@RequestParam String query) {
        List<Project> projects = projectService.searchProjects(query);
        List<ProjectDto> projectDtos = projects.stream()
                .map(ProjectDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(projectDtos);
    }
}
