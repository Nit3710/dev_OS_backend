package com.devos.core.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface FileService {
    
    Map<String, Object> getProjectFiles(Long projectId);
    
    String getFileContent(Long projectId, String filePath);
    
    void setFileContent(Long projectId, String filePath, String content);
    
    Map<String, Object> uploadFile(Long projectId, MultipartFile file, String targetPath);
    
    Map<String, Object> updateFile(Long projectId, String filePath, String content);
    
    Map<String, Object> applyChanges(Long projectId, Map<String, Object> changes);
    
    Map<String, Object> createFile(Long projectId, String filePath, String content);
    
    void deleteFile(Long projectId, String filePath);
    
    void moveFile(Long projectId, String sourcePath, String targetPath);
    
    Object searchInFiles(Long projectId, String query, String filePattern, Boolean caseSensitive);
}
