package com.devos.file.service.impl;

import com.devos.core.service.AuthService;
import com.devos.file.service.DiffService;
import com.devos.file.service.FileService;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("fileDiffServiceImpl")
@RequiredArgsConstructor
@Slf4j
public class DiffServiceImpl implements DiffService {

    private final FileService fileService;
    private final AuthService authService;

    @Override
    public Object generateDiff(Long projectId, String filePath, String newContent, String token) {
        try {
            // Get current file content
            String currentContent = fileService.getFileContent(projectId, filePath, token);
            if (currentContent == null) {
                currentContent = "";
            }
            
            // Generate diff
            return generateUnifiedDiff(currentContent, newContent != null ? newContent : "", filePath);
            
        } catch (Exception e) {
            log.error("Error generating diff for file: {}", filePath, e);
            throw new RuntimeException("Failed to generate diff", e);
        }
    }

    @Override
    public Object compareFiles(Long projectId, String filePath1, String filePath2, String token) {
        try {
            String content1 = fileService.getFileContent(projectId, filePath1, token);
            String content2 = fileService.getFileContent(projectId, filePath2, token);
            
            if (content1 == null) content1 = "";
            if (content2 == null) content2 = "";
            
            Map<String, Object> comparison = new HashMap<>();
            comparison.put("file1", filePath1);
            comparison.put("file2", filePath2);
            comparison.put("diff", generateUnifiedDiff(content1, content2, filePath1 + " vs " + filePath2));
            comparison.put("similarity", calculateSimilarity(content1, content2));
            
            return comparison;
            
        } catch (Exception e) {
            log.error("Error comparing files: {} and {}", filePath1, filePath2, e);
            throw new RuntimeException("Failed to compare files", e);
        }
    }

    private Map<String, Object> generateUnifiedDiff(String original, String revised, String filename) {
        List<String> originalLines = List.of(original.split("\n"));
        List<String> revisedLines = List.of(revised.split("\n"));
        
        Patch<String> patch = DiffUtils.diff(originalLines, revisedLines);
        
        Map<String, Object> diffResult = new HashMap<>();
        diffResult.put("filename", filename);
        diffResult.put("additions", countAdditions(patch));
        diffResult.put("deletions", countDeletions(patch));
        diffResult.put("changes", patch.getDeltas().size());
        
        List<Map<String, Object>> hunks = new ArrayList<>();
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            Map<String, Object> hunk = new HashMap<>();
            hunk.put("type", delta.getType().toString());
            hunk.put("original", Map.of(
                "start", delta.getSource().getPosition(),
                "lines", delta.getSource().getLines(),
                "size", delta.getSource().size()
            ));
            hunk.put("revised", Map.of(
                "start", delta.getTarget().getPosition(),
                "lines", delta.getTarget().getLines(),
                "size", delta.getTarget().size()
            ));
            hunks.add(hunk);
        }
        diffResult.put("hunks", hunks);
        
        // Generate unified diff format
        StringBuilder unifiedDiff = new StringBuilder();
        unifiedDiff.append("--- a/").append(filename).append("\n");
        unifiedDiff.append("+++ b/").append(filename).append("\n");
        
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int originalStart = delta.getSource().getPosition() + 1;
            int revisedStart = delta.getTarget().getPosition() + 1;
            int originalSize = delta.getSource().size();
            int revisedSize = delta.getTarget().size();
            
            unifiedDiff.append("@@ -").append(originalStart);
            if (originalSize != 1) unifiedDiff.append(",").append(originalSize);
            unifiedDiff.append(" +").append(revisedStart);
            if (revisedSize != 1) unifiedDiff.append(",").append(revisedSize);
            unifiedDiff.append(" @@\n");
            
            if (delta.getType() == DeltaType.CHANGE || delta.getType() == DeltaType.DELETE) {
                for (String line : delta.getSource().getLines()) {
                    unifiedDiff.append("-").append(line).append("\n");
                }
            }
            
            if (delta.getType() == DeltaType.CHANGE || delta.getType() == DeltaType.INSERT) {
                for (String line : delta.getTarget().getLines()) {
                    unifiedDiff.append("+").append(line).append("\n");
                }
            }
        }
        
        diffResult.put("unifiedDiff", unifiedDiff.toString());
        
        return diffResult;
    }

    private int countAdditions(Patch<String> patch) {
        return patch.getDeltas().stream()
                .mapToInt(delta -> {
                    if (delta.getType() == DeltaType.INSERT) {
                        return delta.getTarget().size();
                    } else if (delta.getType() == DeltaType.CHANGE) {
                        return delta.getTarget().size();
                    }
                    return 0;
                })
                .sum();
    }

    private int countDeletions(Patch<String> patch) {
        return patch.getDeltas().stream()
                .mapToInt(delta -> {
                    if (delta.getType() == DeltaType.DELETE) {
                        return delta.getSource().size();
                    } else if (delta.getType() == DeltaType.CHANGE) {
                        return delta.getSource().size();
                    }
                    return 0;
                })
                .sum();
    }

    private double calculateSimilarity(String content1, String content2) {
        if (content1.equals(content2)) {
            return 1.0;
        }
        
        List<String> lines1 = List.of(content1.split("\n"));
        List<String> lines2 = List.of(content2.split("\n"));
        
        Patch<String> patch = DiffUtils.diff(lines1, lines2);
        int totalLines = Math.max(lines1.size(), lines2.size());
        int changedLines = countAdditions(patch) + countDeletions(patch);
        
        return totalLines > 0 ? 1.0 - (double) changedLines / totalLines : 1.0;
    }
}
