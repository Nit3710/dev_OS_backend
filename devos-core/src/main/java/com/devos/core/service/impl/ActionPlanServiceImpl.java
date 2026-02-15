package com.devos.core.service.impl;

import com.devos.core.domain.entity.ActionPlan;
import com.devos.core.domain.entity.PlanStep;
import com.devos.core.repository.ActionPlanRepository;
import com.devos.core.repository.PlanStepRepository;
import com.devos.core.service.ActionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionPlanServiceImpl implements ActionPlanService {

    private final ActionPlanRepository actionPlanRepository;
    private final PlanStepRepository planStepRepository;
    private final com.devos.core.service.AuthService authService;

    @Override
    @Transactional
    public ActionPlan createActionPlan(ActionPlan actionPlan) {
        // Assume context validation is done or needed here
        actionPlan.setStatus(ActionPlan.PlanStatus.DRAFT);
        actionPlan.setCreatedAt(LocalDateTime.now());
        
        ActionPlan savedPlan = actionPlanRepository.save(actionPlan);
        log.info("Created action plan: {}", savedPlan.getTitle());
        
        return savedPlan;
    }

    @Override
    public ActionPlan getActionPlan(Long id) {
        return getActionPlanWithOwnership(id);
    }

    @Override
    public List<ActionPlan> getProjectActionPlans(Long projectId) {
        // Ideally verify project ownership here too
        return actionPlanRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Override
    @Transactional
    public ActionPlan updateActionPlan(Long id, ActionPlan actionPlan) {
        ActionPlan existingPlan = getActionPlanWithOwnership(id);
        
        existingPlan.setTitle(actionPlan.getTitle());
        existingPlan.setDescription(actionPlan.getDescription());
        existingPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan updatedPlan = actionPlanRepository.save(existingPlan);
        log.info("Updated action plan: {}", updatedPlan.getTitle());
        
        return updatedPlan;
    }

    @Override
    @Transactional
    public void deleteActionPlan(Long id) {
        ActionPlan actionPlan = getActionPlanWithOwnership(id);
        actionPlanRepository.delete(actionPlan);
        log.info("Deleted action plan: {}", actionPlan.getTitle());
    }

    @Override
    @Transactional
    public ActionPlan approveActionPlan(Long id) {
        ActionPlan actionPlan = getActionPlanWithOwnership(id);
        actionPlan.setStatus(ActionPlan.PlanStatus.APPROVED);
        actionPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan approvedPlan = actionPlanRepository.save(actionPlan);
        log.info("Approved action plan: {}", approvedPlan.getTitle());
        
        return approvedPlan;
    }

    @Override
    @Transactional
    public ActionPlan executeActionPlan(Long id) {
        ActionPlan actionPlan = getActionPlanWithOwnership(id);
        actionPlan.setStatus(ActionPlan.PlanStatus.IN_PROGRESS);
        actionPlan.setExecutedAt(LocalDateTime.now());
        actionPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan executingPlan = actionPlanRepository.save(actionPlan);
        log.info("Started executing action plan: {}", executingPlan.getTitle());
        
        return executingPlan;
    }

    @Override
    @Transactional
    public ActionPlan rollbackActionPlan(Long id) {
        ActionPlan actionPlan = getActionPlanWithOwnership(id);
        actionPlan.setStatus(ActionPlan.PlanStatus.ROLLED_BACK);
        actionPlan.setRollbackAt(LocalDateTime.now());
        actionPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan rolledBackPlan = actionPlanRepository.save(actionPlan);
        log.info("Rolled back action plan: {}", rolledBackPlan.getTitle());
        
        return rolledBackPlan;
    }

    @Override
    @Transactional
    public ActionPlan pauseActionPlan(Long id) {
        ActionPlan actionPlan = getActionPlanWithOwnership(id);
        actionPlan.setStatus(ActionPlan.PlanStatus.PAUSED);
        actionPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan pausedPlan = actionPlanRepository.save(actionPlan);
        log.info("Paused action plan: {}", pausedPlan.getTitle());
        
        return pausedPlan;
    }

    @Override
    @Transactional
    public ActionPlan resumeActionPlan(Long id) {
        ActionPlan actionPlan = getActionPlanWithOwnership(id);
        actionPlan.setStatus(ActionPlan.PlanStatus.IN_PROGRESS);
        actionPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan resumedPlan = actionPlanRepository.save(actionPlan);
        log.info("Resumed action plan: {}", resumedPlan.getTitle());
        
        return resumedPlan;
    }

    @Override
    public List<PlanStep> getPlanSteps(Long planId) {
        getActionPlanWithOwnership(planId); // Verify access
        return planStepRepository.findByActionPlanId(planId);
    }

    @Override
    @Transactional
    public PlanStep retryStep(Long planId, Long stepId) {
        getActionPlanWithOwnership(planId); // Verify access
        PlanStep step = planStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Plan step not found with id: " + stepId));
        
        step.setRetryCount(step.getRetryCount() + 1);
        step.setStatus(PlanStep.StepStatus.PENDING);
        
        PlanStep retriedStep = planStepRepository.save(step);
        log.info("Retried plan step: {}", step.getTitle());
        
        return retriedStep;
    }

    private ActionPlan getActionPlanWithOwnership(Long id) {
        ActionPlan plan = actionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Action plan not found with id: " + id));
        
        com.devos.core.domain.entity.User currentUser = authService.getCurrentUser();
        if (!plan.getProject().getUser().getId().equals(currentUser.getId())) {
             throw new SecurityException("Access denied: You do not own the project associated with this plan");
        }
        return plan;
    }
}
