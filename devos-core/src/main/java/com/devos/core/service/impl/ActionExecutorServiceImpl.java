package com.devos.core.service.impl;

import com.devos.core.domain.entity.ActionPlan;
import com.devos.core.domain.entity.PlanStep;
import com.devos.core.domain.entity.Project;
import com.devos.core.repository.ActionPlanRepository;
import com.devos.core.repository.PlanStepRepository;
import com.devos.core.repository.ProjectRepository;
import com.devos.core.service.ActionExecutorService;
import com.devos.core.service.FileService;
import org.springframework.beans.factory.annotation.Qualifier;
import com.devos.core.service.GitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ActionExecutorServiceImpl implements ActionExecutorService {

    private final ActionPlanRepository actionPlanRepository;
    private final PlanStepRepository planStepRepository;
    private final ProjectRepository projectRepository;
    private final FileService fileService;
    private final GitService gitService;
    private final com.devos.core.repository.FileChangeRepository fileChangeRepository;
    private final com.devos.core.service.AuthService authService;

    public ActionExecutorServiceImpl(
            ActionPlanRepository actionPlanRepository,
            PlanStepRepository planStepRepository,
            ProjectRepository projectRepository,
            @Qualifier("fileOperationsServiceImpl") FileService fileService,
            GitService gitService,
            com.devos.core.repository.FileChangeRepository fileChangeRepository,
            com.devos.core.service.AuthService authService) {
        this.actionPlanRepository = actionPlanRepository;
        this.planStepRepository = planStepRepository;
        this.projectRepository = projectRepository;
        this.fileService = fileService;
        this.gitService = gitService;
        this.fileChangeRepository = fileChangeRepository;
        this.authService = authService;
    }

    @Override
    @Transactional
    public ActionPlan executePlan(Long actionPlanId) {
        log.info("Starting execution of action plan: {}", actionPlanId);
        
        ActionPlan actionPlan = actionPlanRepository.findById(actionPlanId)
                .orElseThrow(() -> new RuntimeException("Action plan not found: " + actionPlanId));
        
        // Security check
        com.devos.core.domain.entity.User currentUser = authService.getCurrentUser();
        if (!actionPlan.getProject().getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Access denied: You do not own the project associated with this plan");
        }

        if (actionPlan.getStatus() != ActionPlan.PlanStatus.APPROVED && 
            actionPlan.getStatus() != ActionPlan.PlanStatus.IN_PROGRESS) {
            throw new IllegalStateException("Plan must be APPROVED or IN_PROGRESS to execute. Current status: " + actionPlan.getStatus());
        }

        actionPlan.setStatus(ActionPlan.PlanStatus.IN_PROGRESS);
        actionPlan.setExecutedAt(LocalDateTime.now());
        actionPlanRepository.save(actionPlan);

        try {
            List<PlanStep> steps = planStepRepository.findByActionPlanId(actionPlanId);
            
            for (PlanStep step : steps) {
                if (step.getStatus() == PlanStep.StepStatus.PENDING || 
                    step.getStatus() == PlanStep.StepStatus.FAILED) {
                    executeStep(step.getId());
                }
            }
            
            actionPlan.setStatus(ActionPlan.PlanStatus.COMPLETED);
            actionPlan.setCompletedAt(LocalDateTime.now());
            log.info("Successfully executed action plan: {}", actionPlanId);
            
        } catch (Exception e) {
            log.error("Execution failed for plan: {}", actionPlanId, e);
            actionPlan.setStatus(ActionPlan.PlanStatus.FAILED);
            // In a real system, we might want to trigger a rollback here
        }
        
        return actionPlanRepository.save(actionPlan);
    }

    @Override
    @Transactional
    public void executeStep(Long stepId) {
        PlanStep step = planStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found: " + stepId));
        
        log.info("Executing step {}: {}", step.getStepNumber(), step.getTitle());
        
        step.setStatus(PlanStep.StepStatus.IN_PROGRESS);
        step.setStartedAt(LocalDateTime.now());
        planStepRepository.save(step);
        
        try {
            Project project = step.getActionPlan().getProject();
            String projectPath = project.getLocalPath();
            Long projectId = project.getId();
            
            switch (step.getType()) {
                case CREATE_FILE:
                    handleCreateFile(projectId, step);
                    break;
                case UPDATE_FILE:
                    handleUpdateFile(projectId, step);
                    break;
                case DELETE_FILE:
                    handleDeleteFile(projectId, step);
                    break;
                case GIT_OPERATION:
                    handleGitOperation(projectPath, step.getParameters());
                    break;
                case RUN_COMMAND:
                    handleRunCommand(projectPath, step);
                    break;
                default:
                    log.warn("Unknown step type: {}", step.getType());
            }
            
            step.setStatus(PlanStep.StepStatus.COMPLETED);
            step.setCompletedAt(LocalDateTime.now());
            step.setErrorMessage(null);
            
        } catch (Exception e) {
            log.error("Failed to execute step: {}", stepId, e);
            step.setStatus(PlanStep.StepStatus.FAILED);
            step.setErrorMessage(e.getMessage());
            step.setRetryCount(step.getRetryCount() + 1);
            throw new RuntimeException("Step execution failed", e);
        } finally {
            planStepRepository.save(step);
        }
    }

    private void handleCreateFile(Long projectId, PlanStep step) {
        Map<String, Object> params = step.getParameters();
        String path = (String) params.get("path");
        String content = (String) params.get("content");
        
        if (path == null || content == null) {
            throw new IllegalArgumentException("Create file requires 'path' and 'content' parameters");
        }
        
        fileService.createFile(projectId, path, content);
        
        saveFileChange(step, path, com.devos.core.domain.entity.FileChange.ChangeType.CREATE, null, content);
    }

    private void handleUpdateFile(Long projectId, PlanStep step) {
        Map<String, Object> params = step.getParameters();
        String path = (String) params.get("path");
        String content = (String) params.get("content");
        
        if (path == null || content == null) {
            throw new IllegalArgumentException("Update file requires 'path' and 'content' parameters");
        }
        
        String oldContent = "";
        try {
            oldContent = fileService.getFileContent(projectId, path);
        } catch (Exception ignored) {
            // File might not exist or other error, treat old content as empty
        }
        
        fileService.setFileContent(projectId, path, content);
        
        saveFileChange(step, path, com.devos.core.domain.entity.FileChange.ChangeType.UPDATE, oldContent, content);
    }

    private void handleDeleteFile(Long projectId, PlanStep step) {
        Map<String, Object> params = step.getParameters();
        String path = (String) params.get("path");
        
        if (path == null) {
            throw new IllegalArgumentException("Delete file requires 'path' parameter");
        }
        
        String oldContent = "";
        try {
            oldContent = fileService.getFileContent(projectId, path);
        } catch (Exception ignored) {}

        fileService.deleteFile(projectId, path);
        
        saveFileChange(step, path, com.devos.core.domain.entity.FileChange.ChangeType.DELETE, oldContent, null);
    }

    private void handleRunCommand(String projectPath, PlanStep step) {
        Map<String, Object> params = step.getParameters();
        String command = (String) params.get("command");
        
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Run command requires 'command' parameter");
        }

        log.info("Running command in {}: {}", projectPath, command);
        
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            // This is a basic shell executor. In a real production environment, 
            // you must use a sandbox like Docker or a MicroVM.
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }
            
            processBuilder.directory(new java.io.File(projectPath));
            processBuilder.redirectErrorStream(true); // Merge stderr into stdout
            
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (output.length() > 50000) { // Safety limit for output size
                        output.append("... [Output Truncated] ...");
                        break;
                    }
                }
            }
            
            int exitCode = process.waitFor();
            step.setOutput(output.toString());
            
            if (exitCode != 0) {
                step.setErrorMessage("Command failed with exit code: " + exitCode);
                // We might set status to FAILED here, but executeStep handles the exception
                throw new RuntimeException("Command failed: " + command + " (Exit code: " + exitCode + ")");
            }
            
        } catch (Exception e) {
            log.error("Error executing command", e);
            throw new RuntimeException("Command execution failed: " + e.getMessage(), e);
        }
    }

    private void saveFileChange(PlanStep step, String path, com.devos.core.domain.entity.FileChange.ChangeType type, String oldContent, String newContent) {
        com.devos.core.domain.entity.FileChange change = com.devos.core.domain.entity.FileChange.builder()
                .actionPlan(step.getActionPlan())
                .planStep(step)
                .filePath(path)
                .type(type)
                .oldContent(oldContent)
                .newContent(newContent)
                .status(com.devos.core.domain.entity.FileChange.ChangeStatus.APPLIED)
                .createdAt(LocalDateTime.now())
                .appliedAt(LocalDateTime.now())
                .build();
        
        fileChangeRepository.save(change);
    }

    private void handleGitOperation(String projectPath, Map<String, Object> params) {
        String operation = (String) params.get("operation");
        
        if ("commit".equalsIgnoreCase(operation)) {
            String message = (String) params.get("message");
            // Values below should ideally come from user context
            String authorName = (String) params.getOrDefault("authorName", "DevOS AI");
            String authorEmail = (String) params.getOrDefault("authorEmail", "ai@devos.local");
            
            gitService.createCommit(projectPath, message, authorName, authorEmail);
        } else if ("branch".equalsIgnoreCase(operation)) {
             String branchName = (String) params.get("name");
             gitService.createBranch(projectPath, branchName);
             gitService.checkoutBranch(projectPath, branchName);
        }
        // Add other git operations as needed
    }
}
