package com.devos.core.service;

import java.util.Map;

public interface FileIndexingService {
    
    void indexProject(Long projectId);
    
    void indexProjectFiles(Long projectId);
    
    Map<String, Object> searchInProject(Long projectId, String query);
    
    void updateIndex(Long projectId, String filePath, String content);
    
    void removeFromIndex(Long projectId, String filePath);
}
