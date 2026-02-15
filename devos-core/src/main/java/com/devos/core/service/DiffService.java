package com.devos.core.service;

import java.util.Map;

public interface DiffService {
    
    Map<String, Object> generateDiff(Long projectId, String filePath, String content);
    
    Map<String, Object> compareFiles(Long projectId, String file1Path, String file2Path);
}
