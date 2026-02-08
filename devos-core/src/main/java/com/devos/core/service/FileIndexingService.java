package com.devos.core.service;

import java.util.Map;

public interface FileIndexingService {
    
    void indexProject(Long projectId, String token);
    
    void indexProjectFiles(Long projectId, String token);
    
    Map<String, Object> searchInProject(Long projectId, String query, String token);
    
    void updateIndex(Long projectId, String filePath, String content, String token);
    
    void removeFromIndex(Long projectId, String filePath, String token);
}
