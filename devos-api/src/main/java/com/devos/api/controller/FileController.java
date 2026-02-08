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
@RequestMapping("/files")
@Slf4j
public class FileController {

    private final FileService fileService;
    private final DiffService diffService;

    public FileController(
            FileService fileService,
            @Qualifier("coreDiffServiceImpl") DiffService diffService) {
        this.fileService = fileService;
        this.diffService = diffService;
    }

    @GetMapping("/{projectId}/content")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<String> getFileContent(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam String filePath) {
        
        String jwtToken = token.replace("Bearer ", "");
        String content = fileService.getFileContent(projectId, filePath, jwtToken);
        
        return ResponseEntity.ok(content);
    }

    @PostMapping("/{projectId}/content")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> setFileContent(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam String filePath,
            @RequestBody String content) {
        
        String jwtToken = token.replace("Bearer ", "");
        fileService.setFileContent(projectId, filePath, content, jwtToken);
        
        log.info("File content updated: {} for project: {}", filePath, projectId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{projectId}/upload")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String targetPath) {
        
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> result = fileService.uploadFile(projectId, file, targetPath, jwtToken);
        
        log.info("File uploaded: {} for project: {}", file.getOriginalFilename(), projectId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{projectId}/diff")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Object> generateDiff(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam String filePath,
            @RequestBody(required = false) String newContent) {
        
        String jwtToken = token.replace("Bearer ", "");
        Object diff = diffService.generateDiff(projectId, filePath, newContent, jwtToken);
        
        return ResponseEntity.ok(diff);
    }

    @PostMapping("/{projectId}/diff/compare")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Object> compareFiles(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam String filePath1,
            @RequestParam String filePath2) {
        
        String jwtToken = token.replace("Bearer ", "");
        Object diff = diffService.compareFiles(projectId, filePath1, filePath2, jwtToken);
        
        return ResponseEntity.ok(diff);
    }

    @PutMapping("/{projectId}/apply")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> applyChanges(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> changes) {
        
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> result = fileService.applyChanges(projectId, changes, jwtToken);
        
        log.info("Changes applied for project: {}", projectId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{projectId}/file")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFile(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam String filePath) {
        
        String jwtToken = token.replace("Bearer ", "");
        fileService.deleteFile(projectId, filePath, jwtToken);
        
        log.info("File deleted: {} for project: {}", filePath, projectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/create")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createFile(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam String filePath,
            @RequestBody(required = false) String content) {
        
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> result = fileService.createFile(projectId, filePath, content, jwtToken);
        
        log.info("File created: {} for project: {}", filePath, projectId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{projectId}/move")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> moveFile(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam String sourcePath,
            @RequestParam String targetPath) {
        
        String jwtToken = token.replace("Bearer ", "");
        fileService.moveFile(projectId, sourcePath, targetPath, jwtToken);
        
        log.info("File moved from {} to {} for project: {}", sourcePath, targetPath, projectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/search")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Object> searchInFiles(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId,
            @RequestParam String query,
            @RequestParam(required = false) String filePattern,
            @RequestParam(required = false) Boolean caseSensitive) {
        
        String jwtToken = token.replace("Bearer ", "");
        Object results = fileService.searchInFiles(projectId, query, filePattern, caseSensitive, jwtToken);
        
        return ResponseEntity.ok(results);
    }
}
