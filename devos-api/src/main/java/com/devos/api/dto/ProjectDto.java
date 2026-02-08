package com.devos.api.dto;

import com.devos.core.domain.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String repositoryUrl;
    private String localPath;
    private Project.ProjectStatus status;
    private Boolean isIndexed;
    private LocalDateTime lastIndexedAt;
    private String gitBranch;
    private String gitCommitHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    
    public static ProjectDto from(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .slug(project.getSlug())
                .description(project.getDescription())
                .repositoryUrl(project.getRepositoryUrl())
                .localPath(project.getLocalPath())
                .status(project.getStatus())
                .isIndexed(project.getIsIndexed())
                .lastIndexedAt(project.getLastIndexedAt())
                .gitBranch(project.getGitBranch())
                .gitCommitHash(project.getGitCommitHash())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .userId(project.getUser() != null ? project.getUser().getId() : null)
                .build();
    }
    
    public Project toEntity() {
        Project project = new Project();
        project.setId(this.id);
        project.setName(this.name);
        project.setSlug(this.slug);
        project.setDescription(this.description);
        project.setRepositoryUrl(this.repositoryUrl);
        project.setLocalPath(this.localPath);
        project.setStatus(this.status);
        project.setIsIndexed(this.isIndexed);
        project.setLastIndexedAt(this.lastIndexedAt);
        project.setGitBranch(this.gitBranch);
        project.setGitCommitHash(this.gitCommitHash);
        project.setCreatedAt(this.createdAt);
        project.setUpdatedAt(this.updatedAt);
        return project;
    }
}
