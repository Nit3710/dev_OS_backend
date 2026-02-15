package com.devos.api.controller;

import com.devos.core.service.FileService;
import com.devos.core.service.DiffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@Slf4j
public class FileController {

    private final FileService fileService;
    private final DiffService diffService;

    public FileController(
            @Qualifier("coreFileServiceImpl") FileService fileService,
            @Qualifier("coreDiffServiceImpl") DiffService diffService) {
        this.fileService = fileService;
        this.diffService = diffService;
    }

    @GetMapping("/{projectId}/content")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<String> getFileContent(
            @PathVariable("projectId") Long projectId,
            @RequestParam("filePath") String filePath) {
        
        String content = fileService.getFileContent(projectId, filePath);
        return ResponseEntity.ok(content);
    }

    @PostMapping("/{projectId}/content")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> setFileContent(
            @PathVariable("projectId") Long projectId,
            @RequestParam("filePath") String filePath,
            @RequestBody String content) {
        
        fileService.setFileContent(projectId, filePath, content);
        
        log.info("File content updated: {} for project: {}", filePath, projectId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{projectId}/upload")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable("projectId") Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "targetPath", required = false) String targetPath) {
        
        Map<String, Object> result = fileService.uploadFile(projectId, file, targetPath);
        
        log.info("File uploaded: {} for project: {}", file.getOriginalFilename(), projectId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{projectId}/diff")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Object> generateDiff(
            @PathVariable("projectId") Long projectId,
            @RequestParam("filePath") String filePath,
            @RequestBody(required = false) String newContent) {
        
        Object diff = diffService.generateDiff(projectId, filePath, newContent);
        
        return ResponseEntity.ok(diff);
    }

    @PostMapping("/{projectId}/diff/compare")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Object> compareFiles(
            @PathVariable("projectId") Long projectId,
            @RequestParam("filePath1") String filePath1,
            @RequestParam("filePath2") String filePath2) {
        
        Object diff = diffService.compareFiles(projectId, filePath1, filePath2);
        
        return ResponseEntity.ok(diff);
    }

    @PutMapping("/{projectId}/apply")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> applyChanges(
            @PathVariable("projectId") Long projectId,
            @RequestBody Map<String, Object> changes) {
        
        Map<String, Object> result = fileService.applyChanges(projectId, changes);
        
        log.info("Changes applied for project: {}", projectId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{projectId}/file")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFile(
            @PathVariable("projectId") Long projectId,
            @RequestParam("filePath") String filePath) {
        
        fileService.deleteFile(projectId, filePath);
        
        log.info("File deleted: {} for project: {}", filePath, projectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/create")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createFile(
            @PathVariable("projectId") Long projectId,
            @RequestParam("filePath") String filePath,
            @RequestBody(required = false) String content) {
        
        Map<String, Object> result = fileService.createFile(projectId, filePath, content);
        
        log.info("File created: {} for project: {}", filePath, projectId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{projectId}/move")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> moveFile(
            @PathVariable("projectId") Long projectId,
            @RequestParam("sourcePath") String sourcePath,
            @RequestParam("targetPath") String targetPath) {
        
        fileService.moveFile(projectId, sourcePath, targetPath);
        
        log.info("File moved from {} to {} for project: {}", sourcePath, targetPath, projectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/search")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Object> searchInFiles(
            @PathVariable("projectId") Long projectId,
            @RequestParam("query") String query,
            @RequestParam(name = "filePattern", required = false) String filePattern,
            @RequestParam(name = "caseSensitive", required = false) Boolean caseSensitive) {
        
        Object results = fileService.searchInFiles(projectId, query, filePattern, caseSensitive);
        
        return ResponseEntity.ok(results);
    }
}
