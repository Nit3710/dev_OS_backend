package com.devos.core.service;

import java.util.List;
import java.util.Map;

public interface GitService {

    String getCurrentCommitHash(String projectPath);

    String createCommit(String projectPath, String message, String authorName, String authorEmail);

    List<Map<String, Object>> getCommitHistory(String projectPath, int limit);

    String createBranch(String projectPath, String branchName);

    void checkoutBranch(String projectPath, String branchName);

    List<String> getBranches(String projectPath);

    String mergeBranch(String projectPath, String sourceBranch, String targetBranch);

    void revertCommit(String projectPath, String commitHash);

    Map<String, Object> getFileStatus(String projectPath);

    List<String> getChangedFiles(String projectPath);

    String stashChanges(String projectPath, String message);

    List<String> getStashList(String projectPath);

    void applyStash(String projectPath, String stashId);

    void discardChanges(String projectPath, String filePath);

    Map<String, Object> getRemoteStatus(String projectPath);

    void pullChanges(String projectPath);

    void pushChanges(String projectPath, String branchName);
}
