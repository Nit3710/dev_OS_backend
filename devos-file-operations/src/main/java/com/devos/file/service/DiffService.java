package com.devos.file.service;

public interface DiffService {

    Object generateDiff(Long projectId, String filePath, String newContent, String token);

    Object compareFiles(Long projectId, String filePath1, String filePath2, String token);
}
