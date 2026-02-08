package com.devos.core.service;

import java.util.Map;

public interface DiffService {
    
    Map<String, Object> generateDiff(Long projectId, String filePath, String content, String token);
    
    Map<String, Object> compareFiles(Long projectId, String file1Path, String file2Path, String token);
}
