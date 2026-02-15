package com.devos.core.service.impl;

import com.devos.core.domain.entity.FileNode;
import com.devos.core.domain.entity.FileOperation;
import com.devos.core.repository.FileNodeRepository;
import com.devos.core.repository.FileOperationRepository;
import com.devos.core.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("coreFileServiceImpl")
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileNodeRepository fileNodeRepository;
    private final FileOperationRepository fileOperationRepository;
    private final com.devos.core.service.AuthService authService;

    @Override
    public Map<String, Object> getProjectFiles(Long projectId) {
        List<FileNode> files = fileNodeRepository.findByProjectId(projectId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("files", files);
        result.put("totalFiles", files.size());
        
        return result;
    }

    @Override
    public String getFileContent(Long projectId, String filePath) {
        FileNode fileNode = fileNodeRepository.findByProjectIdAndRelativePath(projectId, filePath)
                .orElseThrow(() -> new RuntimeException("File not found: " + filePath));
        
        try {
            return Files.readString(Paths.get(fileNode.getAbsolutePath()));
        } catch (IOException e) {
            log.error("Error reading file: {}", filePath, e);
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    @Override
    @Transactional
    public void setFileContent(Long projectId, String filePath, String content) {
        FileNode fileNode = fileNodeRepository.findByProjectIdAndRelativePath(projectId, filePath)
                .orElseThrow(() -> new RuntimeException("File not found: " + filePath));
        
        try {
            Files.writeString(Paths.get(fileNode.getAbsolutePath()), content);
            fileNode.setLastModified(System.currentTimeMillis());
            fileNodeRepository.save(fileNode);
            
            log.info("Updated file content: {}", filePath);
        } catch (IOException e) {
            log.error("Error writing file: {}", filePath, e);
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> uploadFile(Long projectId, MultipartFile file, String targetPath) {
        try {
            String originalFilename = file.getOriginalFilename();
            Path destinationPath = Paths.get(targetPath, originalFilename);
            
            Files.createDirectories(destinationPath.getParent());
            Files.copy(file.getInputStream(), destinationPath);
            
            // Create file node record
            FileNode fileNode = FileNode.builder()
                    .name(originalFilename)
                    .relativePath(targetPath + "/" + originalFilename)
                    .absolutePath(destinationPath.toString())
                    .type(FileNode.FileType.FILE)
                    .fileSize(file.getSize())
                    .build();
            
            fileNodeRepository.save(fileNode);
            
            // Log file operation
            FileOperation operation = FileOperation.builder()
                    .type(FileOperation.OperationType.CREATE)
                    .filePath(targetPath + "/" + originalFilename)
                    .fileSize(file.getSize())
                    .status(FileOperation.OperationStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .projectId(projectId)
                    .build();
            
            fileOperationRepository.save(operation);
            
            Map<String, Object> result = new HashMap<>();
            result.put("filename", originalFilename);
            result.put("path", targetPath + "/" + originalFilename);
            result.put("size", file.getSize());
            result.put("message", "File uploaded successfully");
            
            log.info("Uploaded file: {} to {}", originalFilename, targetPath);
            return result;
            
        } catch (IOException e) {
            log.error("Error uploading file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> updateFile(Long projectId, String filePath, String content) {
        FileNode fileNode = fileNodeRepository.findByProjectIdAndRelativePath(projectId, filePath)
                .orElseThrow(() -> new RuntimeException("File not found: " + filePath));
        
        try {
            Files.writeString(Paths.get(fileNode.getAbsolutePath()), content);
            fileNode.setLastModified(System.currentTimeMillis());
            fileNodeRepository.save(fileNode);
            
            // Log file operation
            FileOperation operation = FileOperation.builder()
                    .type(FileOperation.OperationType.UPDATE)
                    .filePath(filePath)
                    .fileSize((long) content.getBytes().length)
                    .status(FileOperation.OperationStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .projectId(projectId)
                    .build();
            
            fileOperationRepository.save(operation);
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", filePath);
            result.put("message", "File updated successfully");
            result.put("size", content.getBytes().length);
            
            log.info("Updated file: {}", filePath);
            return result;
            
        } catch (IOException e) {
            log.error("Error updating file: {}", filePath, e);
            throw new RuntimeException("Failed to update file: " + filePath, e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> applyChanges(Long projectId, Map<String, Object> changes) {
        log.info("Applying batch changes for project: {}", projectId);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> operations = (List<Map<String, Object>>) changes.get("operations");
        
        if (operations == null || operations.isEmpty()) {
            return Map.of("message", "No operations to apply", "success", true);
        }

        int successCount = 0;
        int failureCount = 0;
        
        for (Map<String, Object> op : operations) {
            String type = (String) op.get("type");
            String path = (String) op.get("path");
            String content = (String) op.get("content");
            
            try {
                switch (type.toUpperCase()) {
                    case "CREATE":
                        createFile(projectId, path, content);
                        successCount++;
                        break;
                    case "UPDATE":
                        updateFile(projectId, path, content);
                        successCount++;
                        break;
                    case "DELETE":
                        deleteFile(projectId, path);
                        successCount++;
                        break;
                    case "MOVE":
                        String target = (String) op.get("target");
                        moveFile(projectId, path, target);
                        successCount++;
                        break;
                    default:
                        log.warn("Unknown operation type in batch: {}", type);
                        failureCount++;
                }
            } catch (Exception e) {
                log.error("Failed to apply batch operation: {} on {}", type, path, e);
                failureCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("totalOperations", operations.size());
        result.put("message", String.format("Batch complete: %d success, %d failure", successCount, failureCount));
        
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createFile(Long projectId, String filePath, String content) {
        try {
            Path fullPath = Paths.get(filePath);
            Files.createDirectories(fullPath.getParent());
            Files.writeString(fullPath, content);
            
            // Create file node record
            FileNode fileNode = FileNode.builder()
                    .name(Paths.get(filePath).getFileName().toString())
                    .relativePath(filePath)
                    .absolutePath(fullPath.toString())
                    .type(FileNode.FileType.FILE)
                    .fileSize((long) content.getBytes().length)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            fileNodeRepository.save(fileNode);
            
            // Log file operation
            FileOperation operation = FileOperation.builder()
                    .type(FileOperation.OperationType.CREATE)
                    .filePath(filePath)
                    .fileSize((long) content.getBytes().length)
                    .status(FileOperation.OperationStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .projectId(projectId)
                    .build();
            
            fileOperationRepository.save(operation);
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", filePath);
            result.put("message", "File created successfully");
            result.put("size", content.getBytes().length);
            
            log.info("Created file: {}", filePath);
            return result;
            
        } catch (IOException e) {
            log.error("Error creating file: {}", filePath, e);
            throw new RuntimeException("Failed to create file: " + filePath, e);
        }
    }

    @Override
    @Transactional
    public void deleteFile(Long projectId, String filePath) {
        FileNode fileNode = fileNodeRepository.findByProjectIdAndRelativePath(projectId, filePath)
                .orElseThrow(() -> new RuntimeException("File not found: " + filePath));
        
        try {
            Files.deleteIfExists(Paths.get(fileNode.getAbsolutePath()));
            fileNodeRepository.delete(fileNode);
            
            // Log file operation
            FileOperation operation = FileOperation.builder()
                    .type(FileOperation.OperationType.DELETE)
                    .filePath(filePath)
                    .status(FileOperation.OperationStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .projectId(projectId)
                    .build();
            
            fileOperationRepository.save(operation);
            
            log.info("Deleted file: {}", filePath);
            
        } catch (IOException e) {
            log.error("Error deleting file: {}", filePath, e);
            throw new RuntimeException("Failed to delete file: " + filePath, e);
        }
    }

    @Override
    @Transactional
    public void moveFile(Long projectId, String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);
            
            Files.createDirectories(target.getParent());
            Files.move(source, target);
            
            // Update source file node
            FileNode sourceNode = fileNodeRepository.findByProjectIdAndRelativePath(projectId, sourcePath)
                    .orElseThrow(() -> new RuntimeException("Source file not found: " + sourcePath));
            
            sourceNode.setRelativePath(targetPath);
            sourceNode.setAbsolutePath(target.toString());
            fileNodeRepository.save(sourceNode);
            
            // Log file operation
            FileOperation operation = FileOperation.builder()
                    .type(FileOperation.OperationType.MOVE)
                    .sourcePath(sourcePath)
                    .destinationPath(targetPath)
                    .status(FileOperation.OperationStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .projectId(projectId)
                    .build();
            
            fileOperationRepository.save(operation);
            
            log.info("Moved file from {} to {}", sourcePath, targetPath);
            
        } catch (IOException e) {
            log.error("Error moving file from {} to {}", sourcePath, targetPath, e);
            throw new RuntimeException("Failed to move file: " + sourcePath, e);
        }
    }

    @Override
    public Object searchInFiles(Long projectId, String query, String filePattern, Boolean caseSensitive) {
        List<FileNode> files = fileNodeRepository.findByProjectIdOrderByRelativePathAsc(projectId);
        
        // Simple search implementation
        String searchLower = query.toLowerCase();
        
        return files.stream()
                .filter(file -> file.getName().toLowerCase().contains(searchLower))
                .limit(10) // Limit results
                .toList();
    }
}
