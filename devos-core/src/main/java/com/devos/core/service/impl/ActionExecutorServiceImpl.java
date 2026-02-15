package com.devos.core.service.impl;

import com.devos.core.domain.entity.ActionPlan;
import com.devos.core.domain.entity.PlanStep;
import com.devos.core.domain.entity.Project;
import com.devos.core.repository.ActionPlanRepository;
import com.devos.core.repository.PlanStepRepository;
import com.devos.core.repository.ProjectRepository;
import com.devos.core.service.ActionExecutorService;
import com.devos.file.service.FileService;
import com.devos.file.service.GitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionExecutorServiceImpl implements ActionExecutorService {

    private final ActionPlanRepository actionPlanRepository;
    private final PlanStepRepository planStepRepository;
    private final ProjectRepository projectRepository;
    private final FileService fileService;
    private final GitService gitService;
    private final com.devos.core.repository.FileChangeRepository fileChangeRepository;

    @Override
    @Transactional
    public ActionPlan executePlan(Long actionPlanId, String token) {
        log.info("Starting execution of action plan: {}", actionPlanId);
        
        ActionPlan actionPlan = actionPlanRepository.findById(actionPlanId)
                .orElseThrow(() -> new RuntimeException("Action plan not found: " + actionPlanId));

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
                    executeStep(step.getId(), token);
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
    public void executeStep(Long stepId, String token) {
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
                    handleCreateFile(projectId, step, token);
                    break;
                case UPDATE_FILE:
                    handleUpdateFile(projectId, step, token);
                    break;
                case DELETE_FILE:
                    handleDeleteFile(projectId, step, token);
                    break;
                case GIT_OPERATION:
                    handleGitOperation(projectPath, step.getParameters());
                    break;
                case RUN_COMMAND:
                    log.warn("RUN_COMMAND step type not yet fully implemented for safety reasons: {}", step.getId());
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

    private void handleCreateFile(Long projectId, PlanStep step, String token) {
        Map<String, Object> params = step.getParameters();
        String path = (String) params.get("path");
        String content = (String) params.get("content");
        
        if (path == null || content == null) {
            throw new IllegalArgumentException("Create file requires 'path' and 'content' parameters");
        }
        
        fileService.createFile(projectId, path, content, token);
        
        saveFileChange(step, path, com.devos.core.domain.entity.FileChange.ChangeType.CREATE, null, content);
    }

    private void handleUpdateFile(Long projectId, PlanStep step, String token) {
        Map<String, Object> params = step.getParameters();
        String path = (String) params.get("path");
        String content = (String) params.get("content");
        
        if (path == null || content == null) {
            throw new IllegalArgumentException("Update file requires 'path' and 'content' parameters");
        }
        
        String oldContent = "";
        try {
            oldContent = fileService.getFileContent(projectId, path, token);
        } catch (Exception ignored) {
            // File might not exist or other error, treat old content as empty
        }
        
        fileService.setFileContent(projectId, path, content, token);
        
        saveFileChange(step, path, com.devos.core.domain.entity.FileChange.ChangeType.UPDATE, oldContent, content);
    }

    private void handleDeleteFile(Long projectId, PlanStep step, String token) {
        Map<String, Object> params = step.getParameters();
        String path = (String) params.get("path");
        
        if (path == null) {
            throw new IllegalArgumentException("Delete file requires 'path' parameter");
        }
        
        String oldContent = "";
        try {
            oldContent = fileService.getFileContent(projectId, path, token);
        } catch (Exception ignored) {}

        fileService.deleteFile(projectId, path, token);
        
        saveFileChange(step, path, com.devos.core.domain.entity.FileChange.ChangeType.DELETE, oldContent, null);
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
