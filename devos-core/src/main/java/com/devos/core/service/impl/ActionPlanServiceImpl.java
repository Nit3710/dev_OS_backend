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

    @Override
    @Transactional
    public ActionPlan createActionPlan(ActionPlan actionPlan, String token) {
        actionPlan.setStatus(ActionPlan.PlanStatus.DRAFT);
        actionPlan.setCreatedAt(LocalDateTime.now());
        
        ActionPlan savedPlan = actionPlanRepository.save(actionPlan);
        log.info("Created action plan: {}", savedPlan.getTitle());
        
        return savedPlan;
    }

    @Override
    public ActionPlan getActionPlan(Long id, String token) {
        return actionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Action plan not found with id: " + id));
    }

    @Override
    public List<ActionPlan> getProjectActionPlans(Long projectId, String token) {
        return actionPlanRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Override
    @Transactional
    public ActionPlan updateActionPlan(Long id, ActionPlan actionPlan, String token) {
        ActionPlan existingPlan = getActionPlanById(id, token);
        
        existingPlan.setTitle(actionPlan.getTitle());
        existingPlan.setDescription(actionPlan.getDescription());
        existingPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan updatedPlan = actionPlanRepository.save(existingPlan);
        log.info("Updated action plan: {}", updatedPlan.getTitle());
        
        return updatedPlan;
    }

    @Override
    @Transactional
    public void deleteActionPlan(Long id, String token) {
        ActionPlan actionPlan = getActionPlanById(id, token);
        actionPlanRepository.delete(actionPlan);
        log.info("Deleted action plan: {}", actionPlan.getTitle());
    }

    @Override
    @Transactional
    public ActionPlan approveActionPlan(Long id, String token) {
        ActionPlan actionPlan = getActionPlanById(id, token);
        actionPlan.setStatus(ActionPlan.PlanStatus.APPROVED);
        actionPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan approvedPlan = actionPlanRepository.save(actionPlan);
        log.info("Approved action plan: {}", approvedPlan.getTitle());
        
        return approvedPlan;
    }

    @Override
    @Transactional
    public ActionPlan executeActionPlan(Long id, String token) {
        ActionPlan actionPlan = getActionPlanById(id, token);
        actionPlan.setStatus(ActionPlan.PlanStatus.IN_PROGRESS);
        actionPlan.setExecutedAt(LocalDateTime.now());
        actionPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan executingPlan = actionPlanRepository.save(actionPlan);
        log.info("Started executing action plan: {}", executingPlan.getTitle());
        
        return executingPlan;
    }

    @Override
    @Transactional
    public ActionPlan rollbackActionPlan(Long id, String token) {
        ActionPlan actionPlan = getActionPlanById(id, token);
        actionPlan.setStatus(ActionPlan.PlanStatus.ROLLED_BACK);
        actionPlan.setRollbackAt(LocalDateTime.now());
        actionPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan rolledBackPlan = actionPlanRepository.save(actionPlan);
        log.info("Rolled back action plan: {}", rolledBackPlan.getTitle());
        
        return rolledBackPlan;
    }

    @Override
    @Transactional
    public ActionPlan pauseActionPlan(Long id, String token) {
        ActionPlan actionPlan = getActionPlanById(id, token);
        actionPlan.setStatus(ActionPlan.PlanStatus.PAUSED);
        actionPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan pausedPlan = actionPlanRepository.save(actionPlan);
        log.info("Paused action plan: {}", pausedPlan.getTitle());
        
        return pausedPlan;
    }

    @Override
    @Transactional
    public ActionPlan resumeActionPlan(Long id, String token) {
        ActionPlan actionPlan = getActionPlanById(id, token);
        actionPlan.setStatus(ActionPlan.PlanStatus.IN_PROGRESS);
        actionPlan.setUpdatedAt(LocalDateTime.now());
        
        ActionPlan resumedPlan = actionPlanRepository.save(actionPlan);
        log.info("Resumed action plan: {}", resumedPlan.getTitle());
        
        return resumedPlan;
    }

    @Override
    public List<PlanStep> getPlanSteps(Long planId, String token) {
        return planStepRepository.findByActionPlanId(planId);
    }

    @Override
    @Transactional
    public PlanStep retryStep(Long planId, Long stepId, String token) {
        PlanStep step = planStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Plan step not found with id: " + stepId));
        
        step.setRetryCount(step.getRetryCount() + 1);
        step.setStatus(PlanStep.StepStatus.PENDING);
        
        PlanStep retriedStep = planStepRepository.save(step);
        log.info("Retried plan step: {}", step.getTitle());
        
        return retriedStep;
    }

    private ActionPlan getActionPlanById(Long id, String token) {
        return actionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Action plan not found with id: " + id));
    }
}
