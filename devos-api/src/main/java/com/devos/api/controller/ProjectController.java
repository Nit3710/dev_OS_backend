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
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Page<ProjectDto>> getProjects(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        String jwtToken = token.replace("Bearer ", "");
        Long userId = projectService.getUserIdFromToken(jwtToken);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Project> projects = projectService.getUserProjects(userId, pageable);
        Page<ProjectDto> projectDtos = projects.map(ProjectDto::from);
        
        return ResponseEntity.ok(projectDtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ProjectDto> getProject(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        String jwtToken = token.replace("Bearer ", "");
        Project project = projectService.getProject(id, jwtToken);
        
        return ResponseEntity.ok(ProjectDto.from(project));
    }

    @PostMapping
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ProjectDto> createProject(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ProjectDto projectDto) {
        
        String jwtToken = token.replace("Bearer ", "");
        Project project = projectService.createProject(projectDto.toEntity(), jwtToken);
        
        log.info("Project created: {} for user ID: {}", project.getName(), project.getUser().getId());
        return ResponseEntity.ok(ProjectDto.from(project));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ProjectDto> updateProject(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @Valid @RequestBody ProjectDto projectDto) {
        
        String jwtToken = token.replace("Bearer ", "");
        Project project = projectService.updateProject(id, projectDto.toEntity(), jwtToken);
        
        log.info("Project updated: {}", project.getName());
        return ResponseEntity.ok(ProjectDto.from(project));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProject(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        String jwtToken = token.replace("Bearer ", "");
        projectService.deleteProject(id, jwtToken);
        
        log.info("Project deleted with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/file-tree")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Object> getFileTree(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeContent) {
        
        String jwtToken = token.replace("Bearer ", "");
        Object fileTree = projectService.getFileTree(id, includeContent, jwtToken);
        
        return ResponseEntity.ok(fileTree);
    }

    @PostMapping("/{id}/index")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> indexProject(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        String jwtToken = token.replace("Bearer ", "");
        fileIndexingService.indexProject(id, jwtToken);
        
        log.info("Project indexing started for ID: {}", id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<List<ProjectDto>> searchProjects(
            @RequestHeader("Authorization") String token,
            @RequestParam String query) {
        
        String jwtToken = token.replace("Bearer ", "");
        Long userId = projectService.getUserIdFromToken(jwtToken);
        
        List<Project> projects = projectService.searchProjects(query, userId);
        List<ProjectDto> projectDtos = projects.stream()
                .map(ProjectDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(projectDtos);
    }
}
