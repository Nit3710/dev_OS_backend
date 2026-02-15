package com.devos.file.service.impl;

import com.devos.core.domain.entity.Project;
import com.devos.core.repository.ProjectRepository;
import com.devos.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

@Service("fileOperationsServiceImpl")
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final ProjectRepository projectRepository;
    private final com.devos.core.service.AuthService authService;

    @Override
    @Transactional(readOnly = true)
    public String getFileContent(Long projectId, String filePath) {
        log.info("Getting file content for project: {}, file: {}", projectId, filePath);
        
        Path fullPath = validateAndResolvePath(projectId, filePath);
        
        try {
            if (!Files.exists(fullPath)) {
                throw new RuntimeException("File not found: " + filePath);
            }
            return Files.readString(fullPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading file: {}", fullPath, e);
            throw new RuntimeException("Failed to read file content", e);
        }
    }

    @Override
    @Transactional
    public void setFileContent(Long projectId, String filePath, String content) {
        log.info("Setting file content for project: {}, file: {}", projectId, filePath);
        
        Path fullPath = validateAndResolvePath(projectId, filePath);
        
        try {
            Files.createDirectories(fullPath.getParent());
            Files.writeString(fullPath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("File content updated: {}", fullPath);
        } catch (IOException e) {
            log.error("Error writing file: {}", fullPath, e);
            throw new RuntimeException("Failed to write file content", e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> uploadFile(Long projectId, MultipartFile file, String targetPath) {
        log.info("Uploading file to project: {}, path: {}, filename: {}", 
                projectId, targetPath, file.getOriginalFilename());
        
        String fileName = file.getOriginalFilename();
        String finalPath = (targetPath.endsWith("/") ? targetPath + fileName : targetPath);
        Path fullPath = validateAndResolvePath(projectId, finalPath);
        
        try {
            Files.createDirectories(fullPath.getParent());
            file.transferTo(fullPath.toFile());
            
            return Map.of(
                    "success", true,
                    "projectId", projectId,
                    "filename", fileName,
                    "path", finalPath,
                    "size", file.getSize(),
                    "message", "File uploaded successfully"
            );
        } catch (IOException e) {
            log.error("Error uploading file: {}", fullPath, e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> applyChanges(Long projectId, Map<String, Object> changes) {
        // This is complex and depends on the structure of 'changes'. 
        // For now, let's assume it's handled by individual calls or refine later.
        throw new UnsupportedOperationException("Batch applyChanges not implemented yet");
    }

    @Override
    @Transactional
    public void deleteFile(Long projectId, String filePath) {
        log.info("Deleting file from project: {}, file: {}", projectId, filePath);
        
        Path fullPath = validateAndResolvePath(projectId, filePath);
        
        try {
            if (Files.isDirectory(fullPath)) {
                try (Stream<Path> pathStream = Files.walk(fullPath)) {
                    pathStream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(java.io.File::delete);
                }
            } else {
                Files.deleteIfExists(fullPath);
            }
            log.info("File/Directory deleted: {}", fullPath);
        } catch (IOException e) {
            log.error("Error deleting file: {}", fullPath, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createFile(Long projectId, String filePath, String content) {
        log.info("Creating file in project: {}, path: {}", projectId, filePath);
        
        setFileContent(projectId, filePath, content);
        
        return Map.of(
                "success", true,
                "projectId", projectId,
                "filePath", filePath,
                "message", "File created successfully"
        );
    }

    @Override
    @Transactional
    public void moveFile(Long projectId, String sourcePath, String targetPath) {
        log.info("Moving file in project: {}, from: {}, to: {}", projectId, sourcePath, targetPath);
        
        Path source = validateAndResolvePath(projectId, sourcePath);
        Path target = validateAndResolvePath(projectId, targetPath);
        
        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("File moved from {} to {}", source, target);
        } catch (IOException e) {
            log.error("Error moving file", e);
            throw new RuntimeException("Failed to move file", e);
        }
    }

    @Override
    public Object searchInFiles(Long projectId, String query, String filePattern, Boolean caseSensitive) {
        // Implementation requires file walking and regex matching
        // Leaving as placeholder for now as it doesn't block execution
        return Map.of("message", "Not implemented yet");
    }

    private Path validateAndResolvePath(Long projectId, String relativePath) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        
        com.devos.core.domain.entity.User currentUser = authService.getCurrentUser();
        if (!project.getUser().getId().equals(currentUser.getId())) {
             throw new SecurityException("Access denied: You do not own this project");
        }
        
        Path projectRoot = Paths.get(project.getLocalPath()).toAbsolutePath().normalize();
        Path resolvedPath = projectRoot.resolve(relativePath).normalize();
        
        if (!resolvedPath.startsWith(projectRoot)) {
            throw new SecurityException("Access denied: Path is outside project directory");
        }
        
        return resolvedPath;
    }
}
