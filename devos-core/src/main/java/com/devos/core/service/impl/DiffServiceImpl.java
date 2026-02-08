package com.devos.core.service.impl;

import com.devos.core.domain.entity.Project;
import com.devos.core.domain.entity.User;
import com.devos.core.repository.ProjectRepository;
import com.devos.core.service.AuthService;
import com.devos.core.service.DiffService;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("coreDiffServiceImpl")
@RequiredArgsConstructor
@Slf4j
public class DiffServiceImpl implements DiffService {

    private final AuthService authService;
    private final ProjectRepository projectRepository;

    @Override
    public Map<String, Object> generateDiff(Long projectId, String filePath, String content, String token) {
        try {
            User user = authService.getCurrentUser(token);
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Verify user has access to project
            if (!project.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Access denied");
            }

            // Get current file content
            String currentContent = getCurrentFileContent(project, filePath);
            
            // Generate diff
            List<String> originalLines = currentContent.lines().toList();
            List<String> revisedLines = content.lines().toList();
            
            List<AbstractDelta<String>> deltas = DiffUtils.diff(originalLines, revisedLines).getDeltas();
            
            Map<String, Object> result = new HashMap<>();
            result.put("filePath", filePath);
            result.put("originalLines", originalLines.size());
            result.put("revisedLines", revisedLines.size());
            result.put("deltas", deltas.stream().map(delta -> {
                Map<String, Object> deltaMap = new HashMap<>();
                deltaMap.put("type", delta.getType().toString());
                deltaMap.put("original", delta.getSource().getLines());
                deltaMap.put("revised", delta.getTarget().getLines());
                return deltaMap;
            }).toList());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error generating diff for file: {} in project: {}", filePath, projectId, e);
            throw new RuntimeException("Failed to generate diff", e);
        }
    }

    @Override
    public Map<String, Object> compareFiles(Long projectId, String file1Path, String file2Path, String token) {
        try {
            User user = authService.getCurrentUser(token);
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Verify user has access to project
            if (!project.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Access denied");
            }

            // Get both file contents
            String content1 = getCurrentFileContent(project, file1Path);
            String content2 = getCurrentFileContent(project, file2Path);
            
            // Generate diff
            List<String> lines1 = content1.lines().toList();
            List<String> lines2 = content2.lines().toList();
            
            List<AbstractDelta<String>> deltas = DiffUtils.diff(lines1, lines2).getDeltas();
            
            Map<String, Object> result = new HashMap<>();
            result.put("file1Path", file1Path);
            result.put("file2Path", file2Path);
            result.put("file1Lines", lines1.size());
            result.put("file2Lines", lines2.size());
            result.put("deltas", deltas.stream().map(delta -> {
                Map<String, Object> deltaMap = new HashMap<>();
                deltaMap.put("type", delta.getType().toString());
                deltaMap.put("original", delta.getSource().getLines());
                deltaMap.put("revised", delta.getTarget().getLines());
                return deltaMap;
            }).toList());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error comparing files: {} and {} in project: {}", file1Path, file2Path, projectId, e);
            throw new RuntimeException("Failed to compare files", e);
        }
    }

    private String getCurrentFileContent(Project project, String filePath) throws IOException {
        // This is a simplified implementation
        // In a real scenario, you would read from the actual file system or database
        Path projectPath = Paths.get(project.getLocalPath());
        Path fullFilePath = projectPath.resolve(filePath);

        if (Files.exists(fullFilePath)) {
            return Files.readString(fullFilePath);
        }
        
        return ""; // Return empty string if file doesn't exist
    }
}
