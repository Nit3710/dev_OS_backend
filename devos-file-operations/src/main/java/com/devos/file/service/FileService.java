package com.devos.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface FileService {

    String getFileContent(Long projectId, String filePath, String token);

    void setFileContent(Long projectId, String filePath, String content, String token);

    Map<String, Object> uploadFile(Long projectId, MultipartFile file, String targetPath, String token);

    Map<String, Object> applyChanges(Long projectId, Map<String, Object> changes, String token);

    void deleteFile(Long projectId, String filePath, String token);

    Map<String, Object> createFile(Long projectId, String filePath, String content, String token);

    void moveFile(Long projectId, String sourcePath, String targetPath, String token);

    Object searchInFiles(Long projectId, String query, String filePattern, Boolean caseSensitive, String token);
}
